package varick

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class EventTests extends FunSpec with BeforeAndAfter {


  describe("Event System") {
    it("can emit events") {

      object Foo extends Event{ }

      val eventname = "connection"

      var state = 0
      Foo.on(eventname, () => { state = 1})
      Foo.emit(eventname)
      assert(state === 1)
    }
  }
}
