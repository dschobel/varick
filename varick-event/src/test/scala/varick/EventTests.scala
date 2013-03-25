package varick

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class EventTests extends FunSpec with BeforeAndAfter {


  describe("Event System") {
    it("can emit events without arguments") {
      object Foo extends Event{ }
      val eventname = "connection"
      var state = 0
      Foo.on(eventname, () => { state += 1})
      assert(state === 0)
      Foo.emit(eventname)
      assert(state === 1)
      Foo.emit(eventname)
      assert(state === 2)
    }

    it("can emit events with arguments") {
      object Foo extends Event{}
      var count = 0
      Foo.on("one", (i:Int) => { assert(i === 100); count += 1})
      Foo.on("two", (s:String) => {assert(s === "foo"); count += 1})
      Foo.emit("one", 100)
      Foo.emit("two", "foo")
      assert(count === 2)
    }
  }
}
