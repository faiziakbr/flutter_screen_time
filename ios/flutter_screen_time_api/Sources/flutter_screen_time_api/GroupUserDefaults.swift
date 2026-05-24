import Foundation

final class GroupUserDefaults {

    public static let shared = GroupUserDefaults()
    private let userDefaults: UserDefaults

    init() {
        self.userDefaults = .standard
    }

    func data(forKey: String) -> Data? {
        userDefaults.data(forKey: forKey)
    }

    func set(_ value: Any?, forKey key: String) {
        userDefaults.set(value, forKey: key)
    }

    func getValue(forKey key: String) -> Any? {
        return userDefaults.data(forKey: key)
    }

    func integer(forKey key: String) -> Int {
        return userDefaults.integer(forKey: key)
    }

    func remove(forKey key: String) {
        userDefaults.removeObject(forKey: key)
    }

    func contains(key: String) -> Bool {
        return userDefaults.object(forKey: key) != nil
    }
}
