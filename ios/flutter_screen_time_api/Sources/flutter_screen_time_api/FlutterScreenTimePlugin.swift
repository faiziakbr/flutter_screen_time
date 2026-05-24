import Flutter
import UIKit
import FamilyControls
import SwiftUI
import ManagedSettings

public class FlutterScreenTimePlugin: NSObject, FlutterPlugin {
    private let store = ManagedSettingsStore()
    private let sharedDefaultsSuite = "group.com.faizan.flutterScreenTimeExample"
    private let shieldConfigurationKey = "shield_configuration_payload"

    private struct ShieldConfigurationPayload: Codable {
        let title: String
        let subtitle: String?
        let primaryButtonLabel: String?
        let secondaryButtonLabel: String?
        let primaryButtonBackgroundColorHex: String?
        let backgroundColorHex: String?
        let backgroundBlurStyle: String?
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_screen_time", binaryMessenger: registrar.messenger())
        let instance = FlutterScreenTimePlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "checkAuthorization":
            let value = checkAuthorization()
            result(value)
        case "authorize":
            Task { @MainActor in
                do {
                    try await self.getAuthorization()
                    result(true)
                } catch {
                    result(FlutterError(code: "AUTHORIZATION_ERROR", message: error.localizedDescription, details: nil))
                }
            }
        case "chooseApps":
            chooseApps()
            result(true)
        case "blockApps":
            blockApps();
            result(true)
        case "setShieldConfiguration":
            guard let arguments = call.arguments as? [String: Any] else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "Expected a configuration object.", details: nil))
                return
            }
            do {
                try saveShieldConfiguration(arguments: arguments)
                result(true)
            } catch {
                result(FlutterError(code: "SHIELD_CONFIG_SAVE_FAILED", message: error.localizedDescription, details: nil))
            }
        case "unblockApps":
            encourageAll()
            result(true)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    public func checkAuthorization() -> Int {
        let status = AuthorizationCenter.shared.authorizationStatus
        
        switch status {
        case .notDetermined:
            return 0
            
        case .denied:
            return -1
            
        case .approved:
            return 1
            
        @unknown default:
            return 0
        }
    }
    
    public func getAuthorization() async throws {
        try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
    }
    
    public func chooseApps() {
        DispatchQueue.main.async {
            let familyController = UIHostingController(
                rootView: ContentView { [weak self] in
//                     self?.blockApps()
                }
            )
            familyController.modalPresentationStyle = .fullScreen
            guard let presenter = self.topViewController() else {
                print("No view controller available to present UIHostingController")
                return
            }
            presenter.present(familyController, animated: true)
        }
    }
    
    public func blockApps() {
        saveDefaultShieldConfigurationIfMissing()

        guard let selectedApps = getSavedFamilyActivitySelection() else {
            print("block apps returned")
            return
        }
        if selectedApps.applicationTokens.isEmpty == false {
            store.shield.applications = selectedApps.applicationTokens
        }
        store.shield.applicationCategories = ShieldSettings
            .ActivityCategoryPolicy
            .specific(
                selectedApps.categoryTokens
            )
        store.shield.webDomainCategories = ShieldSettings
            .ActivityCategoryPolicy
            .specific(
                selectedApps.categoryTokens
            )
    }
    
    public func encourageAll() {
        store.shield.applications = []
        store.shield.applicationCategories = ShieldSettings
            .ActivityCategoryPolicy
            .specific(
                []
            )
        store.shield.webDomainCategories = ShieldSettings
            .ActivityCategoryPolicy
            .specific(
                []
            )
    }
    
    private func getSavedFamilyActivitySelection() -> FamilyActivitySelection? {
        let defaults = GroupUserDefaults.shared
        guard let data = defaults.data(forKey: "application_tokens") else {
            print("getSavedFamilyActivitySelection returned")
            return nil
        }
        var selectedApp: FamilyActivitySelection?
        let decoder = PropertyListDecoder()
        selectedApp = try? decoder.decode(FamilyActivitySelection.self, from: data)
        return selectedApp
    }

    private func saveShieldConfiguration(arguments: [String: Any]) throws {
        guard let title = arguments["title"] as? String, title.isEmpty == false else {
            throw NSError(domain: "flutter_screen_time", code: 400, userInfo: [NSLocalizedDescriptionKey: "title is required and must be non-empty"])
        }

        let payload = ShieldConfigurationPayload(
            title: title,
            subtitle: arguments["subtitle"] as? String,
            primaryButtonLabel: arguments["primaryButtonLabel"] as? String,
            secondaryButtonLabel: arguments["secondaryButtonLabel"] as? String,
            primaryButtonBackgroundColorHex: arguments["primaryButtonBackgroundColorHex"] as? String,
            backgroundColorHex: arguments["backgroundColorHex"] as? String,
            backgroundBlurStyle: arguments["backgroundBlurStyle"] as? String
        )

        guard let sharedDefaults = UserDefaults(suiteName: sharedDefaultsSuite) else {
            throw NSError(domain: "flutter_screen_time", code: 500, userInfo: [NSLocalizedDescriptionKey: "Unable to access shared defaults suite"])
        }

        let encodedPayload = try JSONEncoder().encode(payload)
        sharedDefaults.set(encodedPayload, forKey: shieldConfigurationKey)
    }

    private func saveDefaultShieldConfigurationIfMissing() {
        guard let sharedDefaults = UserDefaults(suiteName: sharedDefaultsSuite) else {
            print("Failed to open shared defaults suite for shield config defaults.")
            return
        }

        if sharedDefaults.data(forKey: shieldConfigurationKey) != nil {
            return
        }

        let defaultPayload = ShieldConfigurationPayload(
            title: "Blocked by flutter_screen_time",
            subtitle: "Usage is restricted right now.",
            primaryButtonLabel: "OK",
            secondaryButtonLabel: nil,
            primaryButtonBackgroundColorHex: "#2563EB",
            backgroundColorHex: "#111827",
            backgroundBlurStyle: "systemUltraThinMaterialDark"
        )

        if let encodedPayload = try? JSONEncoder().encode(defaultPayload) {
            sharedDefaults.set(encodedPayload, forKey: shieldConfigurationKey)
        }
    }
    
    private func topViewController(
        from root: UIViewController? = UIApplication.shared
            .connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
    ) -> UIViewController? {
        if let nav = root as? UINavigationController {
            return topViewController(from: nav.visibleViewController)
        }
        if let tab = root as? UITabBarController {
            return topViewController(from: tab.selectedViewController)
        }
        if let presented = root?.presentedViewController {
            return topViewController(from: presented)
        }
        return root
    }
}
