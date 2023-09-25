package me.chuwy.otusfp

import cats.effect._
import cats.effect.std.Random
import fs2.Chunk
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Server
import org.http4s.HttpRoutes
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt

object App {
  case class Count(count: Int)

  implicit val decoder: Decoder[Count] = deriveDecoder[Count]
  implicit val encoder: Encoder[Count] = deriveEncoder[Count]

  private def slow(chunk: Int, total: Int, time: Int): fs2.Stream[IO, Chunk[Byte]] = {
    fs2.Stream.evalSeq(Random.scalaUtilRandom[IO]
      .flatMap(_.nextBytes(total)
      .map(_.toSeq)))
      .chunkN(chunk)
      .metered(time.second)
  }

  def route(e: Ref[IO, Int]): HttpRoutes[IO] = HttpRoutes.of {

    case GET -> Root / "counter" =>
      e.getAndUpdate(_+1).flatMap(c => Ok(Count(c)))

    case GET -> Root / "slow" / IntVar(chunk) / IntVar(total) / IntVar(time)=>
      if (chunk < 0 || total < 0 || time < 0) BadRequest("Invalid parameters")
      else Ok(slow(chunk, total, time))
  }

  def server(e: Ref[IO, Int]): Resource[IO, Server] = BlazeServerBuilder[IO](global)
    .bindHttp(port = 8080, host = "localhost")
    .withHttpApp(route(e).orNotFound)
    .resource
}