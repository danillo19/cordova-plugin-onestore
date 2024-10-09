package com.example.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import com.onestore.iap.api.IapEnum;
import com.onestore.iap.api.IapResult;
import com.onestore.iap.api.ProductDetail;
import com.onestore.iap.api.PurchaseClient;
import com.onestore.iap.api.PurchaseData;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.app.Activity;
import android.util.Log;

import java.util.Random;



public class OneStorePlugin extends CordovaPlugin {
    private CallbackContext context;

    PurchaseClient mPurchaseClient = null;
    int IAP_API_VERSION = 5;
    int LOGIN_REQUEST_CODE = 1001;
    int PURCHASE_REQUEST_CODE = 2001;
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

    public void init((String publicKey, CallbackContext callbackContext) {
        if(mPurchaseClient == null) {
            getAct();
            mPurchaseClient = new PurchaseClient(mAct, publicKey);
        }

        mPurchaseClient.connect(new PurchaseClient.ServiceConnectionListener() {
            @Override
            public void onConnected() {
                mPurchaseClient.isBillingSupportedAsync(IAP_API_VERSION, new PurchaseClient.BillingSupportedListener() {
                    @Override
                    public void onSuccess() {
                        // 然后通过对托管商品和每月采购历史记录的呼叫接收采购历史记录信息。
                        Log.v(TAG, "isBillingSupportedAsync : RESULT_BILLING_OK");
                        loadPurchases();
                        callbackContext.success();
                    }

                    @Override
                    public void onError(IapResult result) {
                        Log.e(TAG, "isBillingSupportedAsync onError, " + result.toString());
                        callbackContext.error("ONE isBillingSupportedAsync onError!!! " + result.toString());

                        // RESULT_NEED_LOGIN 에러시에 개발사의 애플리키에션 life cycle에 맞춰 명시적으로 원스토어 로그인을 호출합니다.
                        if (IapResult.RESULT_NEED_LOGIN == result) {
                            Log.e(TAG, "Try Login 尝试登录");
                            if (mPurchaseClient.launchLoginFlowAsync(IAP_API_VERSION, mAct, LOGIN_REQUEST_CODE, mLoginFlowListener) == false) {
                                // listener is null
                            }
                        }
                    }

                    @Override
                    public void onErrorRemoteException() {
                        Log.e(TAG, "isBillingSupportedAsync onErrorRemoteException, 원스토어 서비스와 연결을 할 수 없습니다");
                        callbackContext.error("onErrorRemoteException");
                    }

                    @Override
                    public void onErrorSecurityException() {
                        Log.e(TAG, "isBillingSupportedAsync onErrorSecurityException, 비정상 앱에서 결제가 요청되었습니다");
                        callbackContext.error("onErrorSecurityException");
                    }

                    @Override
                    public void onErrorNeedUpdateException() {
                        Log.e(TAG, "isBillingSupportedAsync onErrorNeedUpdateException, 원스토어 서비스앱의 업데이트가 필요합니다");
                        callbackContext.error("onErrorNeedUpdateException");
                    }
                });

            }

            @Override
            public void onDisconnected() {
                Log.v("ONESTORE_TEST", "ONE store Disconnected!!!");
                callbackContext.error("ONE store Disconnected!!!");

            }

            @Override
            public void onErrorNeedUpdateException() {//必须更新到最新的onestore客户端后才能进行支付
                Log.v("ONESTORE_TEST", "ONE store NEED_UPDATE!!!");
                callbackContext.error("ONE store NEED_UPDATE!!!");
            }
        });

    }

    public void purchase(String userID, String productId, CallbackContext callbackContext) {
        cbScope = callbackContext;
        getAct();
        mPurchaseClient.launchPurchaseFlowAsync(IAP_API_VERSION,
            mAct, PURCHASE_REQUEST_CODE, productId, userID,
            IapEnum.ProductType.IN_APP.getType(), generatePayload(), "",
            false, new PurchaseClient.PurchaseFlowListener() {
                @Override
                public void onSuccess(PurchaseData purchaseData) {
                    Log.e(TAG, "launchPurchaseFlowAsy Success!");
                    consumeItem(purchaseData);
                    // callbackContext.success(purchaseData);
                }

                @Override
                public void onError(IapResult result) {
                    Log.e(TAG, "launchPurchaseFlowAsync onError, " + result.toString());
                    callbackContext.error(result.toString());
                }

                @Override
                public void onErrorRemoteException() {
                    Log.e(TAG, "launchPurchaseFlowAsync onError=====onErrorRemoteException");
                    callbackContext.error("onErrorRemoteException");
                }

                @Override
                public void onErrorSecurityException() {
                    Log.e(TAG, "launchPurchaseFlowAsync onError=====onErrorSecurityException");
                    callbackContext.error("onErrorSecurityException");
                }

                @Override
                public void onErrorNeedUpdateException() {
                    Log.e(TAG, "launchPurchaseFlowAsync onError=====onErrorNeedUpdateException");
                    callbackContext.error("onErrorNeedUpdateException");
                }
            }
        );
    }


    // NOTE: 登录判断
    /*
    * PurchaseClient의 launchLoginFlowAsync API (로그인) 콜백 리스너
    */
    PurchaseClient.LoginFlowListener mLoginFlowListener = new PurchaseClient.LoginFlowListener() {
        @Override
        public void onSuccess() {
            Log.d(TAG, "launchLoginFlowAsync onSuccess");
        }

        @Override
        public void onError(IapResult result) {
            Log.e(TAG, "launchLoginFlowAsync onError, " + result.toString());
        }

        @Override
        public void onErrorRemoteException() {
            Log.e(TAG, "launchLoginFlowAsync onError, 원스토어 서비스와 연결을 할 수 없습니다");
        }

        @Override
        public void onErrorSecurityException() {
            Log.e(TAG, "launchLoginFlowAsync onError, 비정상 앱에서 결제가 요청되었습니다");
        }

        @Override
        public void onErrorNeedUpdateException() {
            Log.e(TAG, "launchLoginFlowAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다");
        }
    };


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.context = callbackContext;
        if(action.equals("init")) {
            init(callbackContext);
            return true;
        } else if(action.equals("purchase")) {
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
        loadPurchase(IapEnum.ProductType.IN_APP);
        loadPurchase(IapEnum.ProductType.AUTO);
    }


    /*
     * PurchaseClient의 consumeAsync API (상품소비) 콜백 리스너
     */
    PurchaseClient.ConsumeListener mConsumeListener = new PurchaseClient.ConsumeListener() {
        @Override
        public void onSuccess(PurchaseData purchaseData) {
            Log.d(TAG, "TODO consumeAsync onSuccess, " + purchaseData.toString());
            if(cbScope != null) {
                // cbScope.success(purchaseData.toString());
                cbScope.success(purchaseData.getPurchaseData());

                cbScope = null;
            }
        }

        @Override
        public void onErrorRemoteException() {
            Log.e(TAG, "consumeAsync onError, 원스토어 서비스와 연결을 할 수 없습니다");
        }

        @Override
        public void onErrorSecurityException() {
            Log.e(TAG, "consumeAsync onError, 비정상 앱에서 결제가 요청되었습니다");
        }

        @Override
        public void onErrorNeedUpdateException() {
            Log.e(TAG, "consumeAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다");
        }

        @Override
        public void onError(IapResult result) {
            Log.e(TAG, "consumeAsync onError, " + result.toString());
        }
    };



    // 구매내역조회에서 받아온 관리형상품(inapp)의 경우 Signature 검증을 진행하고, 성공할 경우 상품소비를 진행합니다.
    private void onLoadPurchaseInApp(List<PurchaseData> purchaseDataList) {
        Log.i(TAG, "onLoadPurchaseInApp() :: purchaseDataList - " + purchaseDataList.toString());

        for (PurchaseData purchaseData : purchaseDataList) {
            // boolean result = AppSecurity.verifyPurchase(purchase.getPurchaseData(), purchase.getSignature());

            // if (result) {
                consumeItem(purchaseData);
            // }
        }
    }
    // 관리형상품(inapp)의 구매완료 이후 또는 구매내역조회 이후 소비되지 않는 관리형상품에 대해서 소비를 진행합니다.
    private void consumeItem(final PurchaseData purchaseData) {
        Log.e(TAG, "consumeItem() :: getProductId - " + purchaseData.getProductId() + " getPurchaseId -" + purchaseData.getPurchaseId());

        if (mPurchaseClient == null) {
            Log.d(TAG, "PurchaseClient is not initialized");
            return;
        }

        mPurchaseClient.consumeAsync(IAP_API_VERSION, purchaseData, mConsumeListener);
    }
    /*
     * PurchaseClient의 queryPurchasesAsync API (구매내역조회) 콜백 리스너
     */
    PurchaseClient.QueryPurchaseListener mQueryPurchaseListener = new PurchaseClient.QueryPurchaseListener() {
        @Override
        public void onSuccess(List<PurchaseData> purchaseDataList, String productType) {
            Log.d(TAG, "queryPurchasesAsync onSuccess, " + purchaseDataList.toString());

            if (IapEnum.ProductType.IN_APP.getType().equalsIgnoreCase(productType)) {
                onLoadPurchaseInApp(purchaseDataList);
                Log.i(TAG, "onLoadPurchaseAuto() :: IN_APP purchaseDataList - " + purchaseDataList.toString());

            } else if (IapEnum.ProductType.AUTO.getType().equalsIgnoreCase(productType)) {
                Log.i(TAG, "onLoadPurchaseAuto() :: AUTO purchaseDataList - " + purchaseDataList.toString());

            }
        }

        @Override
        public void onErrorRemoteException() {
            Log.e(TAG, "queryPurchasesAsync onError, 원스토어 서비스와 연결을 할 수 없습니다");
        }

        @Override
        public void onErrorSecurityException() {
            Log.e(TAG, "queryPurchasesAsync onError, 비정상 앱에서 결제가 요청되었습니다");
        }

        @Override
        public void onErrorNeedUpdateException() {
            Log.e(TAG, "queryPurchasesAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다");
        }

        @Override
        public void onError(IapResult result) {
            Log.e(TAG, "queryPurchasesAsync onError, " + result.toString());
        }
    };

    private void loadPurchase(final IapEnum.ProductType productType) {
        Log.i(TAG, "loadPurchase() :: productType - " + productType.getType());
        mPurchaseClient.queryPurchasesAsync(IAP_API_VERSION, productType.getType(), mQueryPurchaseListener);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // 根据resultCode判断处理结果
        Log.e("ONESTORE_TEST", "onActivityResult resultCode " + resultCode);

        if(requestCode == PURCHASE_REQUEST_CODE)
        {
            /*
             * launchPurchaseFlowAsync API 호출 시 전달받은 intent 데이터를 handlePurchaseData를 통하여 응답값을 파싱합니다.
             * 파싱 이후 응답 결과를 launchPurchaseFlowAsync 호출 시 넘겨준 PurchaseFlowListener 를 통하여 전달합니다.
             */

            if (resultCode == Activity.RESULT_OK) {
                if (mPurchaseClient.handlePurchaseData(intent) == false) {
                    Log.e(TAG, "onActivityResult handlePurchaseData false ");
                    // listener is null
                }
            } else {
                // if(cbScope != null) {
                //     cbScope.error(resultCode);
                // }
                Log.e(TAG, "onActivityResult user canceled");

                // user canceled , do nothing..
            }
        }else

        if(resultCode== Activity.RESULT_OK){
            String spot=intent.getStringExtra("spot");
//            context.success(spot);
        }
    }



}
