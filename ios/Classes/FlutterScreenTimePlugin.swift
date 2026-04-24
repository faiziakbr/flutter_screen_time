import Flutter
import UIKit
import FamilyControls
import SwiftUI
import ManagedSettings

public class FlutterScreenTimePlugin: NSObject, FlutterPlugin {
    private let store = ManagedSettingsStore()
    
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
        case "unblockApps":
            encourageAll()
            result(true)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    public func checkAuthorization() -> String {
        let status = AuthorizationCenter.shared.authorizationStatus
        
        switch status {
        case .notDetermined:
            return "Permission not requested yet"
            
        case .denied:
            return "Permission denied"
            
        case .approved:
            return "Permission granted"
            
        @unknown default:
            return ""
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
