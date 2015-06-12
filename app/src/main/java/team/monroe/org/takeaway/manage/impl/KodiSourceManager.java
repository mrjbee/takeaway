package team.monroe.org.takeaway.manage.impl;

import org.monroe.team.android.box.json.Json;
import org.monroe.team.android.box.json.JsonBuilder;
import org.monroe.team.android.box.services.HttpManager;
import org.monroe.team.corebox.log.L;

import java.io.IOException;

import team.monroe.org.takeaway.manage.SourceConfigurationManager;
import team.monroe.org.takeaway.manage.SourceManager;

public class KodiSourceManager implements SourceManager {

    L.Logger log = L.create("KODI.SOURCE.MANAGER");

    private final HttpManager httpManager;

    public KodiSourceManager(HttpManager httpManager) {
        this.httpManager = httpManager;
    }

    private <BodyType> Answer<BodyType> sendAndBuild(Send send, BuildBody<BodyType> build){
        try {
            HttpManager.Response<Json> response = send.doSend();
            Json body = response.body;
            try {
                Answer<BodyType> errorAnswer = extractErrorIfExists(body);
                if (errorAnswer != null) return errorAnswer;
                BodyType bodyObject = build.doBuild(body);
                return new Answer<>(Status.SUCCESS,null,bodyObject);
            }catch (Exception e){
                log.w("Invalid response json", e);
                return new Answer<>(Status.UNSUPPORTED_FORMAT, null, null);
            }

        } catch (HttpManager.BadUrlException e){
            log.w("Error during Kodi communication",e);
            return new Answer<>(Status.BAD_URL, null, null);
        } catch (HttpManager.NoRouteToHostException e) {
            log.w("Error during Kodi communication",e);
            return new Answer<>(Status.NO_ROUTE_TO_HOST, null, null);
        } catch (HttpManager.InvalidBodyFormatException e) {
            log.w("Error during Kodi communication",e);
            return new Answer<>(Status.INVALID_RESPONSE, null, null);
        } catch (IOException e) {
            log.w("Error during Kodi communication",e);
            return new Answer<>(Status.BAD_CONNECTION, null, null);
        }
    }

    @Override
    public Answer<String> getSourceVersion(final SourceConfigurationManager.Configuration sourceConfiguration) {
        return sendAndBuild(new Send() {
            @Override
            public HttpManager.Response<Json> doSend() throws HttpManager.BadUrlException, HttpManager.NoRouteToHostException, HttpManager.InvalidBodyFormatException, IOException {
                return httpManager.post(
                        prepare_Url(sourceConfiguration),
                        prepare_JsonRequest(rpc_request("JSONRPC.Version")),
                        prepare_RequestDetails(),
                        prepare_JsonResponse());
            }
        }, new BuildBody<String>() {
            @Override
            public String doBuild(Json json) {
                Json.JsonObject version = json.asObject("result").asObject("version");
                String versionString = version.value("major",Integer.class)+"."+version.value("minor",Integer.class)+"."+version.value("patch",Integer.class);
                return versionString;
            }
        });
    }

    private <BodyType> Answer<BodyType> extractErrorIfExists(Json body) {
        if (body.asObject().exists("error")){
            Status status = Status.FAILED;
            Json.JsonObject error = body.asObject("error");
            String msg = error.asString("message") + "["+error.value("code",Integer.class)+"]";
            return new Answer<>(status,msg,null);
        }else {
            return null;
        }
    }

    private HttpManager.ResponseWithHeadersBuilder<Json> prepare_JsonResponse() {
        return HttpManager.response_json();
    }

    private HttpManager.ConnectionDetails prepare_RequestDetails() {
        return HttpManager.details();
    }

    private HttpManager.RequestWithHeadersBuilder prepare_JsonRequest(JsonBuilder json) {
        return HttpManager.request_json(JsonBuilder.build(json));
    }


    private JsonBuilder.Object rpc_request(String method) {
        // "jsonrpc": "2.0", "method": "JSONRPC.Version", "id": "take.away"
        return JsonBuilder.object()
                        .field("jsonrpc", "2.0")
                        .field("id", "take.away")
                        .field("method", method);
    }

    private String prepare_Url(SourceConfigurationManager.Configuration sourceConfiguration) {
        StringBuilder builder = new StringBuilder();
        if (!sourceConfiguration.host.toLowerCase().startsWith("http://")){
            builder.append("http://");
        }
        builder.append(sourceConfiguration.host);
        builder.append(":"+sourceConfiguration.port);
        builder.append("/jsonrpc");
        return builder.toString();
    }

    private static interface Send {
        public HttpManager.Response<Json> doSend()  throws HttpManager.BadUrlException, HttpManager.NoRouteToHostException, HttpManager.InvalidBodyFormatException, IOException;
    }

    private static  interface BuildBody<BodyType> {
        public BodyType doBuild(Json json);
    }
}
