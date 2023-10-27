package org.linthaal.ai.services.huggingface

import scala.concurrent.Future
import scala.util.{ Failure, Success }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import org.linthaal.ai.services.AIResponse

/**
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program. If not, see <http://www.gnu.org/licenses/>.
  */
object HuggingFaceTextGenAct {

  import HuggingFaceInferencePromptService._

  sealed trait ChatMsg
  case class Response(textGenRes: Seq[TextGenerationResponse]) extends ChatMsg
  case class ChatFailed(reason: String) extends ChatMsg

  case class AIResponseMessage(message: String, result: Seq[String], temperature: Double = 0.0) extends AIResponse

  def apply(
      promtConf: PromptConfig,
      message: String,
      replyTo: ActorRef[AIResponseMessage],
      temperature: Double = 0.0): Behavior[ChatMsg] = {

    Behaviors.setup[ChatMsg] { ctx =>
      val prtServ: HuggingFaceInferencePromptService = new HuggingFaceInferencePromptService(promtConf)(ctx.system)
      ctx.log.info("sent question... ")
      val time1 = System.currentTimeMillis()

      val futRes: Future[Seq[TextGenerationResponse]] = prtServ.promptCall(message, temperature)

      ctx.pipeToSelf(futRes) {
        case Success(rq) => Response(rq)
        case Failure(rf) => ChatFailed(rf.getMessage)
      }
      asking(replyTo, temperature, message, time1)
    }
  }

  def asking(replyTo: ActorRef[AIResponseMessage], temperature: Double, message: String, time: Long): Behavior[ChatMsg] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case msg: Response =>
          replyTo ! AIResponseMessage(message, msg.textGenRes.map(_.generatedText), temperature)
          val t = System.currentTimeMillis() - time
          ctx.log.info(s"[took $t ms] SUCCESSFUL response: $msg")
          Behaviors.stopped

        case msg: ChatFailed =>
          replyTo ! AIResponseMessage(message, Seq.empty, temperature)
          val t = System.currentTimeMillis() - time
          ctx.log.error(s"[took $t ms] FAILED response: $msg")
          Behaviors.stopped
      }
    }
}
