package com.java.streaming.controllers;

import com.java.streaming.constants.AppConstants;
import com.java.streaming.doa.CustomMessage;
import com.java.streaming.entity.Video;
import com.java.streaming.service.VideoService;
import com.java.streaming.service.impl.VideoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam("file") MultipartFile file, @RequestParam("title") String title, @RequestParam("description") String description
    ) {
        Video video  = Video.builder()
                .title(title)
                .description(description)
                .videoId(UUID.randomUUID().toString())
                .build();
        var savedVideo = videoService.save(video, file);
        if(savedVideo!= null){
            return ResponseEntity.status(HttpStatus.OK).body(video);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CustomMessage.builder().message("Video not uploaded").build());
    }

    @GetMapping("/streaming/{videoId}")
    public ResponseEntity<?> streaming(
            @PathVariable("videoId") String videoId,
            @RequestHeader(value = "Range" , required = true) String range
    ) {
        logger.info(range);
        Video video  = videoService.get(videoId);
        Path path = Paths.get(video.getFilePath());
        var contentType = video.getContentType();
        long fileLength = path.toFile().length();

        long rStart, rEnd ;
        String[] rArr = range.replace("bytes=" , "").split("-");
        rStart = Long.parseLong(rArr[0]);
        rEnd = rStart + AppConstants.CHUNK_SIZE - 1;

        if (rEnd >= fileLength) {
            rEnd  = fileLength - 1;
        }
        logger.info("range start : {}", rStart);
        logger.info("range end : {}", rEnd);

        InputStream inputStream;
        try
        {
            inputStream = Files.newInputStream(path);
            inputStream.skip(rStart);
            long contentLength = rEnd - rStart + 1;
            byte[] data = new byte[(int) contentLength];
            int read = inputStream.read(data, 0, data.length);
            System.out.println("read(number of bytes) : " + read);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rStart + "-" + rEnd + "/" + fileLength);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("X-Content-Type-Options", "nosniff");
            headers.setContentLength(contentLength);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Some Error occur!!");
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllVideo() {
        return ResponseEntity.status(HttpStatus.OK).body(videoService.getAll());
    }


    @GetMapping("/getVideo/{videoId}")
    public ResponseEntity<?> getVideo(  @PathVariable("videoId") String videoId) {
        return ResponseEntity.status(HttpStatus.OK).body(videoService.get(videoId));
    }


    @Value("${file.video.hsl}")
    private String HSL_DIR;

    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> serverMasterFile(
            @PathVariable String videoId
    ) {

//        creating path
        Path path = Paths.get(HSL_DIR, videoId, "master.m3u8");

        System.out.println(path);

        if (!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl"
                )
                .body(resource);


    }

    //serve the segments

    @GetMapping("/{videoId}/{segment}.ts")
    public ResponseEntity<Resource> serveSegments(
            @PathVariable String videoId,
            @PathVariable String segment
    ) {

        // create path for segment
        Path path = Paths.get(HSL_DIR, videoId, segment + ".ts");
        if (!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "video/mp2t"
                )
                .body(resource);

    }



}
