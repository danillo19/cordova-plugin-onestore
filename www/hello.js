/*global cordova, module*/

module.exports = {
    init: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Hello", "init", []);
    },
    purchase: function(uid, pid, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Hello", "purchase", [uid, pid]);
    },
};
