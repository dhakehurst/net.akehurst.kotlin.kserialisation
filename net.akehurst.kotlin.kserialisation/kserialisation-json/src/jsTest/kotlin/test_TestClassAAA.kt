import net.akehurst.kotlin.kserialisation.json.test.TestClassAAA
import kotlin.test.Test
import kotlin.test.assertEquals

class test_TestClassAAA {

    @Test
    fun test() {
        val Cls = TestClassAAA::class.js
        val obj1 = js("""
            var obj1 = new Cls('obj1', null);
            obj1.prop1 = 'should not be possible, val property';
            obj1.prop2 = 1
            obj1.comp = new Cls('obj2', null);
            obj1.refr = new Cls('obj3', null);
            obj1
        """) as TestClassAAA

        assertEquals("obj1",obj1.prop1)
        assertEquals("obj2",obj1.comp?.prop1)
        assertEquals("obj3",obj1.refr?.prop1)
    }

}