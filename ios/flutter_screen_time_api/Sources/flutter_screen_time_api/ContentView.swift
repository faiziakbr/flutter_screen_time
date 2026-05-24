import SwiftUI
import FamilyControls

struct ContentView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject var model = FamilyControlModel.shared
    var onDismiss: (() -> Void)? = nil
    @State var isPresented = false
    var body: some View {
        Color.clear
            .onAppear {
                Task {
                    do {
                        try await model.authorize()
                        isPresented = true
                    } catch {
                        print("Authorization failed: \(error)")
                        dismiss()
                    }
                }
            }
            .familyActivityPicker(
                isPresented: $isPresented,
                selection: $model.selectionToDiscourage
            )
            .onAppear {
                if let saved = model.getSavedFamilyActivitySelection() {
                    model.selectionToDiscourage = saved
                }
            }
            .onChange(of: isPresented) { newValue in
                if newValue == false {
                    dismiss()
                    onDismiss?()
                }
            }
    }
}
