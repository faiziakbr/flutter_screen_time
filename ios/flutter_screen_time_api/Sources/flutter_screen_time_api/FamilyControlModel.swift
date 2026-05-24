import Foundation
import FamilyControls
import DeviceActivity
import ManagedSettings

class FamilyControlModel: ObservableObject {
    static let shared = FamilyControlModel()
    private let store = ManagedSettingsStore()
    private let center = DeviceActivityCenter()
    private let encoder = PropertyListEncoder()

    private init() {}

    var selectionToDiscourage = FamilyActivitySelection() {
        didSet {
            if oldValue != selectionToDiscourage {
//                saveFamilyActivitySelection(selection: selectionToDiscourage)
            } else {
//                saveFamilyActivitySelection(selection: oldValue)
            }
        }
        willSet {
            saveFamilyActivitySelection(selection: newValue)
        }
    }

    func authorize() async throws {
        try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
    }

    private func saveFamilyActivitySelection(selection: FamilyActivitySelection) {
//        let defaults = UserDefaults.standard
        let defaults = GroupUserDefaults.shared

        defaults.set(
            try? encoder.encode(selection),
            forKey: "application_tokens"
        )
    }

    //get saved family activity selection from UserDefault
    func getSavedFamilyActivitySelection() -> FamilyActivitySelection? {
        let defaults = GroupUserDefaults.shared
        guard let data = defaults.data(forKey: "application_tokens") else {
            return nil
        }
//        let defaults = UserDefaults.standard
//        guard let data = defaults.data(forKey: "limitedApplicationTokens") else {
//            return nil
//        }
        var selectedApp: FamilyActivitySelection?
        let decoder = PropertyListDecoder()
        selectedApp = try? decoder.decode(FamilyActivitySelection.self, from: data)

        return selectedApp
    }
}

// extension DeviceActivityName {
//     static let daily = Self("daily")
// }
