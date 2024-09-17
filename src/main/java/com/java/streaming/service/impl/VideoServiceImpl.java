package com.java.streaming.service.impl;

import com.java.streaming.entity.Video;
import com.java.streaming.repository.VideoRepository;
import com.java.streaming.service.VideoService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;


@Service
public class VideoServiceImpl implements VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);

    private final VideoRepository videoRepository;


    @Value("${files.video}")
    String DIR ;

    @Value("${files.video.hsl")
    String HSL_DIR ;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init() {
        File file = new File(DIR);
        if(!file.exists()){
            if(file.mkdir()){
                logger.info("Folder Created !!");
            }
            else
            {
                logger.info("Folder Not Created!!");
            }
        }
        else {
            logger.info("Folder Already Created !!");
        }
        initHSL();
    }

    public void initHSL(){
        File file = new File(HSL_DIR);
        if(!file.exists()){
            if(file.mkdir()){
                logger.info("HSL Folder Created !!");
            }
            else
            {
                logger.info("HSL Folder Not Created!!");
            }
        }
        else {
            logger.info("HSL Folder Already Created !!");
        }
    }

    @Override
    public Video save(Video video, MultipartFile file) {

       try {
           String filename = file.getOriginalFilename();
           String contentType = file.getContentType();
           InputStream inputStream = file.getInputStream();

           String clearFileName = StringUtils.cleanPath(filename != null ? filename : "");
           String clearFolder = StringUtils.cleanPath(DIR != null ? DIR : "videos/");

           Path path = Paths.get(clearFolder, clearFileName);
           video.setFilePath(path.toAbsolutePath().toString());
           video.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

           logger.info("File Path : {}", path.toAbsolutePath());

           Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

           var videoSave = videoRepository.save(video);
           processVideo(videoSave.getVideoId());
           return videoSave;


       } catch (Exception e) {
           return null;
       }
    }

    /**
     * @param videoId
     * @return
     */
    @Override
    public Video get(String videoId) {
        return videoRepository.findById(videoId).orElseThrow(()-> new RuntimeException("Video not found"));
    }

    /**
     * @param title
     * @return
     */
    @Override
    public Video getByTitle(String title) {
        return videoRepository.findByTitle(title).orElseThrow(()-> new RuntimeException("Video not found"));
    }

    /**
     * @return
     */
    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }


    @Override
    public String processVideo(String videoId) {

        Video video = this.get(videoId);

        Path videoPath = Paths.get(video.getFilePath());

        try{
            Path outputPath = Paths.get(HSL_DIR, videoId);
            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath, outputPath, outputPath
            );

            logger.info("FFMPEG COMMAND !! {}", ffmpegCmd);
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if(exit != 0){
                throw new RuntimeException("Video Processing Failed!!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return "";
    }
}
