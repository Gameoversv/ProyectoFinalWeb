package servicios;

import edu.pucmm.grpc.*;
import io.grpc.stub.StreamObserver;
import modelo.Url;
import modelo.User;
import URLSnap.app.MainApp;

import java.util.List;

public class UrlServiceImpl extends UrlServiceGrpc.UrlServiceImplBase {

    @Override
    public void listUserUrls(UrlRequest request, StreamObserver<ListUserUrlsResponse> responseObserver) {
        User user = MainApp.userDAO.getUserByUsername(request.getUsername());
        ListUserUrlsResponse.Builder response = ListUserUrlsResponse.newBuilder();

        if (user != null) {
            List<Url> urls = MainApp.urlDAO.getUrlsByUserId(user.getId().toString());
            for (Url url : urls) {
                UrlRecord.Builder urlBuilder = UrlRecord.newBuilder()
                        .setOriginalUrl(url.getOriginalUrl())
                        .setShortUrl("http://localhost:7000/" + url.getShortCode())
                        .setCreatedAt(url.getCreatedAt().toString())
                        .setPreviewBase64(url.getPreviewImage() == null ? "" : url.getPreviewImage());

                for (modelo.Stats s : url.getStats()) {
                    urlBuilder.addStats(
                            Stats.newBuilder()
                                    .setAccessedAt(s.getAccessedAt().toInstant().toString())
                                    .setIpAddress(s.getIpAddress())
                                    .setBrowser(s.getBrowser())
                                    .setOs(s.getOs())
                                    .setReferrer(s.getReferrer())
                                    .setCountry(s.getCountry() == null ? "" : s.getCountry())
                                    .build()
                    );
                }

                response.addUrls(urlBuilder.build());
            }
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void createUrl(CreateUrlRequest request, StreamObserver<CreateUrlResponse> responseObserver) {
        User user = MainApp.userDAO.getUserByUsername(request.getUsername());
        if (user == null) {
            responseObserver.onError(new Throwable("Usuario no encontrado"));
            return;
        }

        Url urlExistente = MainApp.urlDAO.getUrlByOriginalUrlAndUserId(request.getOriginalUrl(), user.getId().toString());
        Url nuevaUrl;

        if (urlExistente != null) {
            nuevaUrl = urlExistente;
        } else {
            nuevaUrl = new Url();
            nuevaUrl.setOriginalUrl(request.getOriginalUrl());
            nuevaUrl.setUserId(user.getId().toString());
            nuevaUrl.setShortCode(MainApp.urlDAO.generarCodigoUnico());
            nuevaUrl.setCreatedAt(new java.util.Date());
            nuevaUrl.setStats(new java.util.ArrayList<>());
            nuevaUrl.setPreviewImage(util.PreviewUtil.fetchPreviewImageBase64(request.getOriginalUrl()));
            MainApp.urlDAO.addUrl(nuevaUrl);
        }

        UrlRecord urlRecord = UrlRecord.newBuilder()
                .setOriginalUrl(nuevaUrl.getOriginalUrl())
                .setShortUrl("http://localhost:7000/" + nuevaUrl.getShortCode())
                .setCreatedAt(nuevaUrl.getCreatedAt().toString())
                .setPreviewBase64(nuevaUrl.getPreviewImage() == null ? "" : nuevaUrl.getPreviewImage())
                .build();

        responseObserver.onNext(CreateUrlResponse.newBuilder().setUrl(urlRecord).build());
        responseObserver.onCompleted();
    }
}
