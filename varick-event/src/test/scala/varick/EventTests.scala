package varick

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class EventTests extends FunSpec with BeforeAndAfter {


  describe("Event System") {
    it("can emit events without arguments") {
      object Foo extends Event{ }
      val eventname = "connection"
      var state = 0
      Foo.on(eventname, () => { state = 1})
      assert(state === 0)
      Foo.emit(eventname)
      assert(state === 1)
    }

    it("can emit events with arguments") {
      object Foo extends EventWithArgs{}
      Foo.on("one", (i:Int) => {assert(i === 100)})
      Foo.on("two", (s:String) => {assert(s === "foo")})
      Foo.emit("one", 103)
      Foo.emit("two", "bar")
    }
  }
}
