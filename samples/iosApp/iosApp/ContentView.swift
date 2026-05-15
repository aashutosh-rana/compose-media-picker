// Copyright 2026 Aashutosh Kumar
// Licensed under the Apache License, Version 2.0.

import SwiftUI
import UIKit
import ComposeApp // The Kotlin/Native framework produced by :samples:composeApp.

/// Hosts the Compose UI inside a SwiftUI scene. The shared Kotlin entry point
/// `MainViewController()` returns a `UIViewController` that already wires the media-picker
/// library against itself, so there is nothing else for the Swift side to set up.
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
