package com.example.capstone.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@NoArgsConstructor
@Getter
@Entity
@Data
@Setter
@Table(name = "board")
public class Board extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String writer;

    @Column(nullable = false)
    private String content;

    @Column
    private String boardtype;

    @Column
    private String updateDate;

    public Board(String title, String password, String writer, String content, String boardtype, String updateDate) {
        this.title = title;
        this.password = password;
        this.writer = writer;
        this.content = content;
        this.boardtype = boardtype;
        this.updateDate = updateDate;
    }
}