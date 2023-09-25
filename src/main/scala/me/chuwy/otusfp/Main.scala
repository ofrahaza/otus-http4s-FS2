package me.chuwy.otusfp


import cats.effect.{IO, IOApp, Ref}


object Main extends IOApp.Simple {

  def run = {
     Ref.of[IO, Int](0).map{ e =>
       App.server(e).use { _ => IO.never}
     }
  }
}
