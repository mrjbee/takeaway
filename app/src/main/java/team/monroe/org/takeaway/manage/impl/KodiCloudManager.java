package team.monroe.org.takeaway.manage.impl;

import org.monroe.team.android.box.json.Json;
import org.monroe.team.android.box.json.JsonBuilder;
import org.monroe.team.android.box.services.HttpManager;
import org.monroe.team.corebox.log.L;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import team.monroe.org.takeaway.manage.CloudConfigurationManager;
import team.monroe.org.takeaway.manage.CloudManager;
import team.monroe.org.takeaway.manage.DownloadManager;

public class KodiCloudManager implements CloudManager {

    L.Logger log = L.create("KODI.SOURCE.MANAGER");

    private final HttpManager httpManager;

    public KodiCloudManager(HttpManager httpManager) {
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
    public Answer<String> getSourceVersion(final CloudConfigurationManager.Configuration sourceConfiguration) {
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

    @Override
    public Answer<List<RemoteFile>> getSources(final CloudConfigurationManager.Configuration sourceConfiguration) {
        return sendAndBuild(new Send() {
            @Override
            public HttpManager.Response<Json> doSend() throws HttpManager.BadUrlException, HttpManager.NoRouteToHostException, HttpManager.InvalidBodyFormatException, IOException {
                return httpManager.post(
                        prepare_Url(sourceConfiguration),
                        prepare_JsonRequest(
                                rpc_request("Files.GetSources")
                                    .field("params", JsonBuilder.object()
                                            .field("media", "music"))),
                        prepare_RequestDetails(),
                        prepare_JsonResponse()
                );
            }
        }, new BuildBody<List<RemoteFile>>() {
            @Override
            public List<RemoteFile> doBuild(Json json) {

                if (!json.asObject("result").exists("sources")) return Collections.emptyList();

                Json.JsonArray sources = json.asObject("result").asArray("sources");

                List<RemoteFile> answer = new ArrayList<RemoteFile>();
                for (int i=0;i<sources.size();i++){
                    Json.JsonObject source = sources.asObject(i);
                    String path = source.asString("file");
                    if (!path.toLowerCase().startsWith("addons:")){
                        answer.add(new RemoteFile(
                                source.asString("file"),
                                source.asString("label"),
                                true));
                    }
                }
                return answer;
            }
        });
    }

    @Override
    public Answer<List<RemoteFile>> getFolderContent(final CloudConfigurationManager.Configuration sourceConfiguration, final String folder) {
        return sendAndBuild(new Send() {
            @Override
            public HttpManager.Response<Json> doSend() throws HttpManager.BadUrlException, HttpManager.NoRouteToHostException, HttpManager.InvalidBodyFormatException, IOException {
                return httpManager.post(
                        prepare_Url(sourceConfiguration),
                        prepare_JsonRequest(
                                //"params":{"directory":"/mnt/bigdata/Musik/Mark 2009/", "media":"music"}}'
                                rpc_request("Files.GetDirectory")
                                        .field("params", JsonBuilder.object()
                                                .field("directory", folder)
                                                .field("media", "music"))),
                        prepare_RequestDetails(),
                        prepare_JsonResponse()
                );
            }
        }, new BuildBody<List<RemoteFile>>() {
            @Override
            public List<RemoteFile> doBuild(Json json) {

                if (!json.asObject("result").exists("files")) return Collections.emptyList();

                Json.JsonArray sources = json.asObject("result").asArray("files");

                List<RemoteFile> answer = new ArrayList<RemoteFile>();
                //"file":"/mnt/bigdata/Musik/Mark 2009/10-so_far_from_the_clyde.mp3","filetype":"file","label":"10-so_far_from_the_clyde.mp3","type":"unknown"
                for (int i=0;i<sources.size();i++){
                    Json.JsonObject source = sources.asObject(i);
                    String path = source.asString("file");
                    File pathAsFile = new File(path);
                    answer.add(new RemoteFile(
                            source.asString("file"),
                            pathAsFile.getName(),
                            "directory".equals(source.asString("filetype")))
                    );
                }
                return answer;
            }
        });
    }

    @Override
    public Answer<DownloadManager.Transfer> createTransfer(final CloudConfigurationManager.Configuration sourceConfiguration, final String fileId) {
        return sendAndBuild(new Send() {
            @Override
            public HttpManager.Response<Json> doSend() throws HttpManager.BadUrlException, HttpManager.NoRouteToHostException, HttpManager.InvalidBodyFormatException, IOException {
                return httpManager.post(
                        prepare_Url(sourceConfiguration),
                        prepare_JsonRequest(
                                rpc_request("Files.PrepareDownload")
                                        .field("params", JsonBuilder.object()
                                                .field("path", fileId))),
                        prepare_RequestDetails(),
                        prepare_JsonResponse()
                );
            }
        }, new BuildBody<DownloadManager.Transfer>() {
            @Override
            public DownloadManager.Transfer doBuild(Json json) {
                if (!json.asObject("result").exists("details")) return null;
                if (!json.asObject("result").asObject("details").exists("path")) return null;
                //TODO: check protocol and vfs type
                String vfsPath = json.asObject("result").asObject("details").asString("path");

                return new KodiFileTransfer(prepare_VfsUrl(sourceConfiguration, vfsPath));
            }
        });
    }

    @Override
    public Answer<Map<String, String>> getFileDetailsMap(final CloudConfigurationManager.Configuration sourceConfiguration, final String fileId) {

        return sendAndBuild(new Send() {
            @Override
            public HttpManager.Response<Json> doSend() throws HttpManager.BadUrlException, HttpManager.NoRouteToHostException, HttpManager.InvalidBodyFormatException, IOException {
                return httpManager.post(
                        prepare_Url(sourceConfiguration),
                        prepare_JsonRequest(
                                rpc_request("Files.GetFileDetails")
                                        .field("params",
                                                JsonBuilder.object()
                                                    .field("file", fileId)
                                                    .field("media", "music")
                                                    .field("properties", JsonBuilder.array()
                                                                            .add("artist")
                                                                            .add("album")
                                                                            .add("title")))),
                        prepare_RequestDetails(),
                        prepare_JsonResponse()
                );
            }
        }, new BuildBody<Map<String, String>>() {
            @Override
            public Map<String, String> doBuild(Json json) {
                if (!json.asObject("result").exists("filedetails")) return null;
                Json.JsonObject fileDetails = json.asObject("result").asObject("filedetails");
                HashMap<String, String> answer =new HashMap<String, String>();
                String album = fileDetails.asString("album");
                String title = fileDetails.asString("title");

                if (album != null && !album.isEmpty()){
                    answer.put("album", album);
                }
                if (title != null && !title.isEmpty()){
                    answer.put("title", title);
                }

                if (!fileDetails.exists("artist")){
                    return answer;
                }

                Json.JsonArray artists = fileDetails.asArray("artist");
                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < artists.size(); i++){
                    String artist = artists.asString(i);
                    if (artist != null && !artist.isEmpty()) {
                        builder.append(" & "+artist.trim());
                    }
                }
                if (builder.length() > 0){
                    answer.put("artist", builder.substring(3));
                }
                return answer;
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

    private String prepare_Url(CloudConfigurationManager.Configuration sourceConfiguration) {
        StringBuilder builder = new StringBuilder();
        if (!sourceConfiguration.host.toLowerCase().startsWith("http://")){
            builder.append("http://");
        }
        builder.append(sourceConfiguration.host);
        builder.append(":"+sourceConfiguration.port);
        builder.append("/jsonrpc");
        return builder.toString();
    }


    private String prepare_VfsUrl(CloudConfigurationManager.Configuration sourceConfiguration, String vfsPath) {
        StringBuilder builder = new StringBuilder();
        if (!sourceConfiguration.host.toLowerCase().startsWith("http://")){
            builder.append("http://");
        }
        builder.append(sourceConfiguration.host);
        builder.append(":"+sourceConfiguration.port);
        builder.append("/"+vfsPath);
        return builder.toString();
    }

    private static interface Send {
        public HttpManager.Response<Json> doSend()  throws HttpManager.BadUrlException, HttpManager.NoRouteToHostException, HttpManager.InvalidBodyFormatException, IOException;
    }

    private static  interface BuildBody<BodyType> {
        public BodyType doBuild(Json json);
    }

    private final class KodiFileTransfer implements DownloadManager.Transfer{

        private final String vfsPath;
        private HttpManager.Response<InputStream> mInputStreamResponse;

        private KodiFileTransfer(String vfsPath) {
            this.vfsPath = vfsPath;
        }

        @Override
        public InputStream getInputStream() throws DownloadManager.TransferFailException {
            HttpManager httpManager = new HttpManager();
            try {
                mInputStreamResponse = httpManager.get(vfsPath, HttpManager.details(), HttpManager.response_input());
                return mInputStreamResponse.body;
            } catch (HttpManager.InvalidBodyFormatException e) {
                throw new DownloadManager.TransferFailException(e);
            } catch (IOException e) {
                throw new DownloadManager.TransferFailException(e);
            }
        }

        @Override
        public void releaseInput() {
            if (mInputStreamResponse != null){
                mInputStreamResponse.release();
            }
        }

    }

}
