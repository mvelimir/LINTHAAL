package org.linthaal.ai.services.openai

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

import scala.concurrent.Future
import scala.util.{ Failure, Success }
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
object OpenAIChatAct {

  import OpenAIPromptService._

  sealed trait ChatMsg
  final case class Response(chatRes: ChatResponse) extends ChatMsg
  final case class ChatFailed(reason: String) extends ChatMsg

  case class AIResponseMessage(
      id: String,
      chatObject: String,
      created: Long,
      choices: Seq[Choice],
      messages: Seq[Message],
      temperature: Double = 0.0,
      model: String)
      extends AIResponse

  def apply(
      promptConf: PromptConfig,
      messages: Seq[Message],
      replyTo: ActorRef[AIResponseMessage],
      temperature: Double = 0.0): Behavior[ChatMsg] = {

    Behaviors.setup[ChatMsg] { ctx =>
      val prtServ: OpenAIPromptService = new OpenAIPromptService(promptConf)(ctx.system)
      ctx.log.info("sent question... ")
      val time1 = System.currentTimeMillis()

      val futRes: Future[ChatResponse] = prtServ.openAIPromptCall(messages, temperature)

      ctx.pipeToSelf(futRes) {
        case Success(rq) => Response(rq)
        case Failure(rf) => ChatFailed(rf.getMessage)
      }
      asking(replyTo = replyTo, model = promptConf.model, temperature = temperature, messages = messages, time = time1)
    }
  }

  def asking(
      replyTo: ActorRef[AIResponseMessage],
      model: String,
      temperature: Double,
      messages: Seq[Message],
      time: Long): Behavior[ChatMsg] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case msg: Response =>
          replyTo ! AIResponseMessage(
            msg.chatRes.id,
            msg.chatRes.chatObject,
            msg.chatRes.created,
            msg.chatRes.choices,
            messages,
            temperature,
            model)
          val t = System.currentTimeMillis() - time
          ctx.log.info(s"[took $t ms] SUCCESSFUL response: $msg")
          Behaviors.stopped

        case msg: ChatFailed =>
          replyTo ! AIResponseMessage("Failed!!", msg.reason, System.currentTimeMillis(), Seq.empty, messages, temperature, model)
          val t = System.currentTimeMillis() - time
          ctx.log.error(s"[took $t ms] FAILED response: $msg")
          Behaviors.stopped
      }
    }
}
