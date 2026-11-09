// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "FoxDesk",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .library(name: "FoxDeskCore", targets: ["FoxDeskCore"]),
        .executable(name: "fox-desk", targets: ["FoxDeskApp"])
    ],
    targets: [
        .target(
            name: "FoxDeskCore",
            path: "Sources/FoxDeskCore",
            linkerSettings: [
                .linkedLibrary("sqlite3")
            ]
        ),
        .executableTarget(
            name: "FoxDeskApp",
            dependencies: ["FoxDeskCore"],
            path: "Sources/FoxDeskApp"
        ),
        .testTarget(
            name: "FoxDeskTests",
            dependencies: ["FoxDeskCore"],
            path: "Tests/FoxDeskTests"
        )
    ]
)
