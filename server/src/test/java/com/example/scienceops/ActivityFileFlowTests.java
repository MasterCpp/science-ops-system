package com.example.scienceops;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "science-ops.storage.local-path=./target/test-storage"
)
class ActivityFileFlowTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void adminCanUploadCoverReferenceItListPreviewDownloadAndDeleteFiles() {
        String token = login("activityadmin");
        String activityId = createActivity(token, "File Activity");

        Map<?, ?> cover = data(upload(token, activityId, "COVER", "cover.png", pngBytes()));
        String coverId = (String) cover.get("id");
        assertThat(cover.get("category")).isEqualTo("COVER");
        assertThat(cover.get("mimeType")).isEqualTo("image/png");

        ResponseEntity<Map> updatedActivity = exchange(HttpMethod.PUT, "/api/admin/activities/" + activityId, token, activityPayload("File Activity", coverId));
        assertThat(updatedActivity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(updatedActivity).get("coverFileId")).isEqualTo(coverId);

        Map<?, ?> attachment = data(upload(token, activityId, "ATTACHMENT", "plan.pdf", "%PDF-1.4".getBytes()));
        String attachmentId = (String) attachment.get("id");

        Map<?, ?> photo = data(upload(token, activityId, "PHOTO", "photo.jpg", jpgBytes()));
        String photoId = (String) photo.get("id");

        ResponseEntity<Map> photoList = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/files?category=PHOTO&keyword=photo", token, null);
        assertThat(photoList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(photoList).get("total")).isEqualTo(1);

        ResponseEntity<byte[]> preview = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/files/" + photoId + "/preview",
                HttpMethod.GET,
                authorized(token),
                byte[].class
        );
        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(preview.getBody()).contains(jpgBytes());

        ResponseEntity<byte[]> download = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/files/" + attachmentId + "/download",
                HttpMethod.GET,
                authorized(token),
                byte[].class
        );
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(download.getBody())).contains("%PDF-1.4");

        ResponseEntity<Map> deleted = exchange(HttpMethod.DELETE, "/api/admin/files/" + attachmentId, token, null);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> afterDelete = exchange(HttpMethod.GET, "/api/admin/activities/" + activityId + "/files?category=ATTACHMENT", token, null);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(afterDelete).get("total")).isEqualTo(0);
    }

    @Test
    void unsupportedAndOversizedUploadsReturnBusinessErrorsAndVolunteerAdminIsDenied() {
        String token = login("activityadmin");
        String volunteerToken = login("volunteeradmin");
        String activityId = createActivity(token, "File Validation Activity");

        ResponseEntity<Map> unsupported = upload(token, activityId, "PHOTO", "notes.txt", "hello".getBytes());
        assertThat(unsupported.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(unsupported.getBody().get("code")).isEqualTo("UNSUPPORTED_FILE_TYPE");

        ResponseEntity<Map> oversized = upload(token, activityId, "PHOTO", "large.jpg", new byte[10 * 1024 * 1024 + 1]);
        assertThat(oversized.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(oversized.getBody().get("code")).isEqualTo("FILE_TOO_LARGE");

        ResponseEntity<Map> denied = upload(volunteerToken, activityId, "PHOTO", "photo.jpg", jpgBytes());
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void photoZipContainsOnlyNonDeletedPhotos() throws IOException {
        String token = login("activityadmin");
        String activityId = createActivity(token, "Photo Zip Activity");
        String keptId = (String) data(upload(token, activityId, "PHOTO", "kept.jpg", jpgBytes())).get("id");
        String deletedId = (String) data(upload(token, activityId, "PHOTO", "deleted.png", pngBytes())).get("id");
        exchange(HttpMethod.DELETE, "/api/admin/files/" + deletedId, token, null);

        ResponseEntity<byte[]> zip = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/activities/" + activityId + "/photos.zip",
                HttpMethod.GET,
                authorized(token),
                byte[].class
        );
        assertThat(zip.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(zipEntryNames(zip.getBody())).contains(keptId + "-kept.jpg").doesNotContain(deletedId + "-deleted.png");
    }

    private ResponseEntity<Map> upload(String token, String activityId, String category, String filename, byte[] bytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("category", category);
        body.add("file", new NamedByteArrayResource(bytes, filename));
        return restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/activities/" + activityId + "/files",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    private String createActivity(String token, String title) {
        return (String) data(exchange(HttpMethod.POST, "/api/admin/activities", token, activityPayload(title, null))).get("id");
    }

    private Map<String, Object> activityPayload(String title, String coverFileId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("coverFileId", coverFileId);
        payload.put("description", "Activity description");
        payload.put("startTime", "2026-11-01T09:00:00");
        payload.put("endTime", "2026-11-01T12:00:00");
        payload.put("location", "Science Hall");
        payload.put("capacity", 50);
        payload.put("registrationDeadline", "2026-10-31T18:00:00");
        payload.put("ownerName", "Ops Owner");
        payload.put("contactPhone", "13800000000");
        payload.put("planContent", "Plan content");
        return payload;
    }

    private String login(String username) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/admin/auth/login",
                json(Map.of("username", username, "password", "password123")),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) data(response).get("token");
    }

    private ResponseEntity<Map> exchange(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "http://localhost:" + port + path,
                method,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private Map<?, ?> data(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<?, ?>) response.getBody().get("data");
    }

    private byte[] pngBytes() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    }

    private byte[] jpgBytes() {
        return new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
    }

    private java.util.List<String> zipEntryNames(byte[] bytes) throws IOException {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
