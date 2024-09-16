package com.java.streaming.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.java.streaming.entity.Video;

@Repository
public interface VideoRepository  extends JpaRepository<Video, String>{

    Optional<Video> findByTitle(String title);
    
}
