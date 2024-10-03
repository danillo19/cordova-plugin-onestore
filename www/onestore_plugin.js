/*global cordova, module*/

module.exports = {
    init: function(publicKey, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "OneStorePlugin", "init", [publicKey]);
    },
    purchase: function(uid, pid, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "OneStorePlugin", "purchase", [uid, pid]);
    },
};
