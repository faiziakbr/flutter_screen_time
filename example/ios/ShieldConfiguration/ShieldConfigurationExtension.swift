//
//  ShieldConfigurationExtension.swift
//  ShieldConfiguration
//
//  Created by Faizan on 08/05/2026.
//

import ManagedSettings
import ManagedSettingsUI
import UIKit

// Override the functions below to customize the shields used in various situations.
// The system provides a default appearance for any methods that your subclass doesn't override.
// Make sure that your class name matches the NSExtensionPrincipalClass in your Info.plist.
class ShieldConfigurationExtension: ShieldConfigurationDataSource {
    private let sharedDefaultsSuite = "group.com.faizan.flutterScreenTimeExample"
    private let shieldConfigurationKey = "shield_configuration_payload"

    private struct ShieldPayload: Codable {
        let title: String
        let subtitle: String?
        let primaryButtonLabel: String?
        let secondaryButtonLabel: String?
        let primaryButtonBackgroundColorHex: String?
        let backgroundColorHex: String?
        let backgroundBlurStyle: String?
    }

    private var payload: ShieldPayload {
        guard
            let sharedDefaults = UserDefaults(suiteName: sharedDefaultsSuite),
            let data = sharedDefaults.data(forKey: shieldConfigurationKey),
            let decoded = try? JSONDecoder().decode(ShieldPayload.self, from: data)
        else {
            return ShieldPayload(
                title: "Shield Active",
                subtitle: "Usage is restricted right now.",
                primaryButtonLabel: "OK",
                secondaryButtonLabel: nil,
                primaryButtonBackgroundColorHex: "#2563EB",
                backgroundColorHex: "#111827",
                backgroundBlurStyle: "systemUltraThinMaterialDark"
            )
        }
        return decoded
    }

    private func makeConfiguration() -> ShieldConfiguration {
        let current = payload
        return ShieldConfiguration(
            backgroundBlurStyle: blurStyle(from: current.backgroundBlurStyle),
            backgroundColor: color(from: current.backgroundColorHex),
            title: ShieldConfiguration.Label(text: current.title, color: .white),
            subtitle: label(from: current.subtitle),
            primaryButtonLabel: label(from: current.primaryButtonLabel),
            primaryButtonBackgroundColor: color(from: current.primaryButtonBackgroundColorHex),
            secondaryButtonLabel: label(from: current.secondaryButtonLabel)
        )
    }

    private func label(from text: String?) -> ShieldConfiguration.Label? {
        guard let text, text.isEmpty == false else {
            return nil
        }
        return ShieldConfiguration.Label(text: text, color: .white)
    }

    private func blurStyle(from value: String?) -> UIBlurEffect.Style {
        switch value {
        case "systemThinMaterial":
            return .systemThinMaterial
        case "systemMaterial":
            return .systemMaterial
        case "systemChromeMaterial":
            return .systemChromeMaterial
        case "systemUltraThinMaterialDark":
            return .systemUltraThinMaterialDark
        case "systemThinMaterialDark":
            return .systemThinMaterialDark
        case "systemMaterialDark":
            return .systemMaterialDark
        case "systemChromeMaterialDark":
            return .systemChromeMaterialDark
        default:
            return .systemUltraThinMaterialDark
        }
    }

    private func color(from hex: String?) -> UIColor? {
        guard var hex else { return nil }
        hex = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if hex.hasPrefix("#") { hex.removeFirst() }
        guard hex.count == 6 || hex.count == 8 else { return nil }

        var value: UInt64 = 0
        guard Scanner(string: hex).scanHexInt64(&value) else { return nil }

        if hex.count == 6 {
            let red = CGFloat((value & 0xFF0000) >> 16) / 255.0
            let green = CGFloat((value & 0x00FF00) >> 8) / 255.0
            let blue = CGFloat(value & 0x0000FF) / 255.0
            return UIColor(red: red, green: green, blue: blue, alpha: 1.0)
        }

        let red = CGFloat((value & 0xFF000000) >> 24) / 255.0
        let green = CGFloat((value & 0x00FF0000) >> 16) / 255.0
        let blue = CGFloat((value & 0x0000FF00) >> 8) / 255.0
        let alpha = CGFloat(value & 0x000000FF) / 255.0
        return UIColor(red: red, green: green, blue: blue, alpha: alpha)
    }

    override func configuration(shielding application: Application) -> ShieldConfiguration {
        makeConfiguration()
    }
    
    override func configuration(shielding application: Application, in category: ActivityCategory) -> ShieldConfiguration {
        makeConfiguration()
    }
    
    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration {
        makeConfiguration()
    }
    
    override func configuration(shielding webDomain: WebDomain, in category: ActivityCategory) -> ShieldConfiguration {
        makeConfiguration()
    }
}
