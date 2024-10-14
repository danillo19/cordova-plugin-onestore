package com.example.plugin;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.gaa.sdk.iap.AcknowledgeListener;
import com.gaa.sdk.iap.AcknowledgeParams;
import com.gaa.sdk.iap.ConsumeListener;
import com.gaa.sdk.iap.ConsumeParams;
import com.gaa.sdk.iap.IapResult;
import com.gaa.sdk.iap.IapResultListener;
import com.gaa.sdk.iap.PurchaseClient;
import com.gaa.sdk.iap.PurchaseClient.ProductType;
import com.gaa.sdk.iap.PurchaseClientStateListener;
import com.gaa.sdk.iap.PurchaseData;
import com.gaa.sdk.iap.PurchaseFlowParams;
import com.gaa.sdk.iap.PurchasesListener;
import com.gaa.sdk.iap.PurchasesUpdatedListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Random;


public class OneStorePlugin extends CordovaPlugin {
    private CallbackContext context;

    PurchaseClient mPurchaseClient = null;
    ConsumeListener mConsumeListener = null;
    String TAG = "ONESTORE_TEST";

    Activity mAct;

    CallbackContext cbScope;

    public String generatePayload() {
        char[] payload;
        final char[] specials = {'~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '-', '{', '}', '|', '\\', '/', '.',
                '.', '=', '[', ']', '?', '<', '>'};
        StringBuilder buffer = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch) {
            buffer.append(ch);
        }
        for (char ch = 'a'; ch <= 'z'; ++ch) {
            buffer.append(ch);
        }
        for (char ch = 'A'; ch <= 'Z'; ++ch) {
            buffer.append(ch);
        }

        for (char ch : specials) {
            buffer.append(ch);
        }

        payload = buffer.toString().toCharArray();

        StringBuilder randomString = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            randomString.append(payload[random.nextInt(payload.length)]);
        }

        return randomString.toString();
    }

    public void getAct() {
        cordova.setActivityResultCallback(this); // Required
        mAct = this.cordova.getActivity();
    }

    public void init(String publicKey, CallbackContext callbackContext) {
        if (mPurchaseClient == null) {
            getAct();

            mPurchaseClient = PurchaseClient.newBuilder(mAct).setBase64PublicKey(publicKey).setListener(new PurchasesUpdatedListener() {
                @Override
                public void onPurchasesUpdated(IapResult iapResult, @Nullable List<PurchaseData> purchases) {
                    if (iapResult.isSuccess() && purchases != null) {
                        for (PurchaseData purchase : purchases) {
                            Log.d(TAG, "successful onPurchasesUpdated; product: " + purchase.getProductId());
                            consumeItem(purchase);
                        }
                    } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_UPDATE) {
                        mPurchaseClient.launchUpdateOrInstallFlow(mAct, new IapResultListener() {
                            @Override
                            public void onResponse(IapResult iapResult) {
                                if (iapResult.isSuccess()) {
                                    Log.i(TAG, "launchUpdateOrInstallFlow was successful");
                                    mPurchaseClient.startConnection(new PurchaseClientStateListener() {
                                        @Override
                                        public void onSetupFinished(IapResult iapResult) {
                                            if (iapResult.isSuccess()) {
                                                loadPurchases();
                                            }
                                        }

                                        @Override
                                        public void onServiceDisconnected() {
                                            Log.e(TAG, "ONE store service disconnected!");
                                        }
                                    });
                                } else {
                                    Log.i(TAG, "launchUpdateOrInstallFlow failed: " + iapResult.getMessage() + " ;code: " + iapResult.getResponseCode());
                                }
                            }
                        });
                    } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_LOGIN) {
                        mPurchaseClient.launchLoginFlowAsync(mAct,
                                iap -> Log.d(TAG, "onResponse of IapResultListener: " + iap.getMessage() + " ;code: " + iap.getResponseCode()));
                    } else {
                        Log.e(TAG, "one store onPurchasesUpdate response msg: " + iapResult.getMessage() + "; code: " + iapResult.getResponseCode());
                    }
                }
            }).build();
        }

        mConsumeListener = (iapResult, purchaseData) -> {
            Log.d(TAG, "TODO consumeAsync onSuccess, " + purchaseData.toString());
            if (cbScope != null) {
                cbScope.success(purchaseData.toString());
                cbScope = null;
            }
        };

        mPurchaseClient.startConnection(new PurchaseClientStateListener() {
            @Override
            public void onSetupFinished(IapResult iapResult) {
                if (iapResult.isSuccess()) {
                    loadPurchases();
                } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_UPDATE) {
                    mPurchaseClient.launchUpdateOrInstallFlow(mAct, null);
                } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_LOGIN) {
                    mPurchaseClient.launchLoginFlowAsync(mAct, new IapResultListener() {
                        @Override
                        public void onResponse(IapResult iapResult) {
                            Log.i(TAG, iapResult.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Code: " + iapResult.getResponseCode() + "; msg: " + iapResult.getMessage());
                    
                }
            }

            @Override
            public void onServiceDisconnected() {
                Log.e(TAG, "ONE store service disconnected!");
            }
        });
    }

    public void purchase(String userID, String productId, CallbackContext callbackContext) {
        cbScope = callbackContext;
        getAct();

        PurchaseFlowParams purchaseFlowParams = PurchaseFlowParams.newBuilder()
                .setProductId(productId)
                .setDeveloperPayload(generatePayload())
                .setQuantity(1)
                .setGameUserId(userID)
                .build();

        mPurchaseClient.launchPurchaseFlow(mAct, purchaseFlowParams);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.context = callbackContext;
        if (action.equals("init")) {
            String publicKey = args.getString(0);
            init(publicKey, callbackContext);
            return true;
        } else if (action.equals("purchase")) {
            String uid = args.getString(0);
            String pid = args.getString(1);
            purchase(uid, pid, callbackContext);
            return true;
        } else {

            return false;

        }

    }

    private void loadPurchases() {
        Log.d("ONESTORE_TEST", "loadPurchases()");
        loadPurchase(ProductType.AUTO);
        loadPurchase(ProductType.INAPP);
    }

    private void onLoadPurchaseInApp(List<PurchaseData> purchaseDataList) {
        Log.i(TAG, "onLoadPurchaseInApp() :: purchaseDataList - " + purchaseDataList.toString());
        ;

        for (PurchaseData purchaseData : purchaseDataList) {
            //boolean result = AppSecurity.verifyPurchase(purchaseData.g(), purchase.getSignature());
            consumeItem(purchaseData);
        }
    }

    private void consumeItem(final PurchaseData purchaseData) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseData(purchaseData)
                .build();


        Log.e(TAG, "consumeItem() :: productId - " + purchaseData.getProductId() + " orderId -" + purchaseData.getOrderId());

        if (mPurchaseClient == null) {
            Log.d(TAG, "PurchaseClient is not initialized");
            return;
        }

        mPurchaseClient.consumeAsync(consumeParams, mConsumeListener);
    }

    PurchasesListener mQueryPurchaseListener = (iapResult, list) -> {
        if (iapResult.isFailure()) {
            Log.d(TAG, "onPurchasesResponse failed: " + iapResult.getMessage());
            return;
        }

        if (list == null) {
            Log.d(TAG, "empty purchasesData list in onPurchasesResponse");
            return;
        }

        Log.d(TAG, "queryPurchasesAsync onSuccess, " + list);
        onLoadPurchaseInApp(list);
    };

    private void loadPurchase(String productType) {
        Log.i(TAG, "loadPurchase() :: productType - " + productType);
        mPurchaseClient.queryPurchasesAsync(productType, mQueryPurchaseListener);
    }

    private void handlePurchase(PurchaseData purchase) {
        if (purchase.getPurchaseState() == PurchaseData.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgeParams acknowledgeParams = AcknowledgeParams.newBuilder()
                        .setPurchaseData(purchase)
                        .build();

                mPurchaseClient.acknowledgeAsync(acknowledgeParams, new AcknowledgeListener() {
                    @Override
                    public void onAcknowledgeResponse(IapResult iapResult, PurchaseData purchaseData) {
                        Log.d(TAG, "onAcknowledgeResponse response: " + iapResult.getMessage());

                    }
                });
            }
        }
    }


}
