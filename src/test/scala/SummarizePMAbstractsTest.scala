import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.linthaal.tot.pubmed.PubMedSumAct
import org.linthaal.tot.pubmed.PubMedSumAct.SummarizationResponse
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

/**
  *
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
  *
  */

class SummarizePMAbstractsTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  //#definition

  "AI to summarize abstracts based on a query " must {
    val timeout = 30.seconds
    //#test
    " reply with a list of summarized abstracts. " in {
      val replyProbe = createTestProbe[SummarizationResponse]()
      val underTest = spawn(PubMedSummarizationAct("pancreatic cancer biomarkers", replyTo = replyProbe.ref))
      replyProbe.expectMessageType[SummarizationResponse](timeout)
    }
  }
}




