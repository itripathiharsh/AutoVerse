// BRouter 1.7.10 routing engine (MIT, https://github.com/abrensch/brouter):
// util, codec, expressions, mapaccess and core merged into one plain Java
// module, unmodified. It computes car routes on the head unit itself from
// downloaded .rd5 road-data tiles — no routing server, no per-request cost.
plugins {
    `java-library`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
    options.encoding = "UTF-8"
    // Upstream code; keep the build output readable.
    options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:none"))
}
