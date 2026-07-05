/// Android-only styling for the full-screen overlay ("shield") that is drawn on
/// top of a blocked app.
///
/// Every colour is a hex string accepting `#RGB`, `#RRGGBB` or `#RRGGBBAA`
/// (alpha last). Any field left `null` keeps its built-in default, so you can
/// override just the pieces you care about.
///
/// This has no effect on iOS — use `ShieldConfigurationPayload` with
/// `FlutterScreenTime.setShieldConfiguration` for the iOS shield.
class AndroidOverlayConfiguration {
  /// Bold heading shown at the top of the overlay.
  final String title;

  /// Supporting line beneath the title. Hidden when null/empty.
  final String? message;

  /// Overlay background fill. Defaults to `#111827`.
  final String? backgroundColorHex;

  /// Colour of the [title] text. Defaults to `#FFFFFF`.
  final String? titleColorHex;

  /// Colour of the [message] text. Defaults to `#D1D5DB`.
  final String? messageColorHex;

  /// Label for the primary (dismiss) button. Defaults to `OK`.
  final String? primaryButtonLabel;

  /// Fill colour of the primary button. Defaults to `#2563EB`.
  final String? primaryButtonBackgroundColorHex;

  /// Text colour of the primary button. Defaults to `#FFFFFF`.
  final String? primaryButtonTextColorHex;

  /// Label for the optional secondary button. Hidden when null/empty.
  final String? secondaryButtonLabel;

  /// Fill colour of the secondary button. When null the button is outlined
  /// (transparent fill with a border in the text colour).
  final String? secondaryButtonBackgroundColorHex;

  /// Text colour of the secondary button. Defaults to `#FFFFFF`.
  final String? secondaryButtonTextColorHex;

  /// Title text size in sp. Defaults to `24`.
  final double? titleFontSize;

  /// Message text size in sp. Defaults to `16`.
  final double? messageFontSize;

  /// Corner radius (dp) applied to the buttons. Defaults to `12`.
  final double? cornerRadius;

  const AndroidOverlayConfiguration({
    required this.title,
    this.message,
    this.backgroundColorHex,
    this.titleColorHex,
    this.messageColorHex,
    this.primaryButtonLabel,
    this.primaryButtonBackgroundColorHex,
    this.primaryButtonTextColorHex,
    this.secondaryButtonLabel,
    this.secondaryButtonBackgroundColorHex,
    this.secondaryButtonTextColorHex,
    this.titleFontSize,
    this.messageFontSize,
    this.cornerRadius,
  });

  Map<String, dynamic> toMap() {
    return {
      "title": title,
      "message": message,
      "backgroundColorHex": backgroundColorHex,
      "titleColorHex": titleColorHex,
      "messageColorHex": messageColorHex,
      "primaryButtonLabel": primaryButtonLabel,
      "primaryButtonBackgroundColorHex": primaryButtonBackgroundColorHex,
      "primaryButtonTextColorHex": primaryButtonTextColorHex,
      "secondaryButtonLabel": secondaryButtonLabel,
      "secondaryButtonBackgroundColorHex": secondaryButtonBackgroundColorHex,
      "secondaryButtonTextColorHex": secondaryButtonTextColorHex,
      "titleFontSize": titleFontSize,
      "messageFontSize": messageFontSize,
      "cornerRadius": cornerRadius,
    };
  }
}
