package cn.nua.wenguan.schedule;
import org.json.JSONObject;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public final class ValidateSignedUpdateManifest {
    static int checks;
    interface Action {void run()throws Exception;}
    static void check(boolean condition){checks++;if(!condition)throw new AssertionError("Manifest check failed");}
    static void reject(Action action)throws Exception{try{action.run();throw new AssertionError("Invalid manifest accepted");}catch(AssertionError failed){throw failed;}catch(Exception expected){checks++;}}
    static byte[] signed(JSONObject release,KeyPair key)throws Exception{
        byte[] payload=release.toString().getBytes(StandardCharsets.UTF_8);Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(key.getPrivate());signer.update(payload);
        return new JSONObject().put("algorithm","SHA256withRSA").put("payload",Base64.getEncoder().encodeToString(payload)).put("signature",Base64.getEncoder().encodeToString(signer.sign())).toString().getBytes(StandardCharsets.UTF_8);
    }
    static JSONObject release(){return new JSONObject().put("schemaVersion",1).put("packageName","cn.nua.wenguan.schedule").put("versionCode",34).put("versionName","1.2.5").put("notes","更新测试，不发布").put("apk",new JSONObject().put("size",100).put("sha256","a".repeat(64)).put("url","https://github.com/HerenYU2007/NUA-Schedule-Android/releases/download/v1.2.5/nua-schedule-1.2.5.apk"));}
    public static void main(String[] args)throws Exception{
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair key=generator.generateKeyPair();
        byte[] good=signed(release(),key);check(AppUpdates.verifyManifest(good,key.getPublic(),"cn.nua.wenguan.schedule").getLong("versionCode")==34);
        reject(()->AppUpdates.verifyManifest(good,generator.generateKeyPair().getPublic(),"cn.nua.wenguan.schedule"));
        reject(()->AppUpdates.verifyManifest(good,key.getPublic(),"other.package"));
        for(String field:new String[]{"schemaVersion","packageName","versionCode","versionName"}){
            JSONObject invalid=release();invalid.put(field,field.equals("schemaVersion")?2:field.equals("versionCode")?0:"invalid");
            reject(()->AppUpdates.verifyManifest(signed(invalid,key),key.getPublic(),"cn.nua.wenguan.schedule"));
        }
        for(String field:new String[]{"size","sha256","url"}){
            JSONObject invalid=release();invalid.getJSONObject("apk").put(field,field.equals("size")?200L*1024*1024+1:"invalid");
            reject(()->AppUpdates.verifyManifest(signed(invalid,key),key.getPublic(),"cn.nua.wenguan.schedule"));
        }
        JSONObject tampered=new JSONObject(new String(good,StandardCharsets.UTF_8));tampered.put("payload",Base64.getEncoder().encodeToString(release().put("versionCode",999).toString().getBytes(StandardCharsets.UTF_8)));
        byte[] badPayload=tampered.toString().getBytes(StandardCharsets.UTF_8);
        reject(()->AppUpdates.verifyManifest(badPayload,key.getPublic(),"cn.nua.wenguan.schedule"));
        tampered=new JSONObject(new String(good,StandardCharsets.UTF_8));tampered.put("algorithm","none");byte[] badAlgorithm=tampered.toString().getBytes(StandardCharsets.UTF_8);
        reject(()->AppUpdates.verifyManifest(badAlgorithm,key.getPublic(),"cn.nua.wenguan.schedule"));
        reject(()->AppUpdates.verifyManifest("<html>not an update</html>".getBytes(StandardCharsets.UTF_8),key.getPublic(),"cn.nua.wenguan.schedule"));
        System.out.println("Actual manifest verifier: "+checks+" checks passed (RSA, tamper, package/version, size/hash, fixed release URL, algorithm, HTML rejection). Test keys stay in memory.");
    }
}
