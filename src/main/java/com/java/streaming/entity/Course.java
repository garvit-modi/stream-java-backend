package com.java.streaming.entity;




import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Entity
@Table(name = "courses")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    @Id
    private String id ;
    private String title ;
    @OneToMany(mappedBy = "course")
    private List<Video> list = new ArrayList<>() ;
}
