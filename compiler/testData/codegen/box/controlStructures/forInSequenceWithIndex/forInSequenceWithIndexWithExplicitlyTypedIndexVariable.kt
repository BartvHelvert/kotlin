// WITH_STDLIB
// KT-55458
// IGNORE_BACKEND_K2: NATIVE

val xs = listOf("a", "b", "c", "d").asSequence()

fun useAny(x: Any) {}

fun box(): String {
    val s = StringBuilder()

    for ((index: Any, x) in xs.withIndex()) {
        useAny(index)
        s.append("$index:$x;")
    }

    val ss = s.toString()
    return if (ss == "0:a;1:b;2:c;3:d;") "OK" else "fail: '$ss'"
}