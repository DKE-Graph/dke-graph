package com.example.capstone.service;

import com.example.capstone.entity.Board;
import com.example.capstone.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BoardService {
    @Autowired
    private BoardRepository boardRepository; // 객체를 생성 - 스프링 부트에서 제공하는 Autowired를 사용시 스프링이 자동으로 의존성 주입

    public void write(Board board) {
        boardRepository.save(board); // 이렇게 생성한 서비스는 다시 컨트롤러에서 사용해야 함
    }

    public Page<Board> boardlist(Pageable pageable) {
        return boardRepository.findAll(pageable); // Board라는 class가 담긴 list를 찾아 반환
    }

    public Board boardview(Long id) {
        return boardRepository.findById(id).get(); // entitiy 속 Long형의 변수를 통해 불러오기에 위에 Long 자료형의 인자를 id로 준다.
    }
    public void boardDelete(Long id) {
        boardRepository.deleteById(id);
    }

    public Page<Board> boardSearchList(String searchKeyword, Pageable pageable) {
        return boardRepository.findByTitleContaining(searchKeyword,pageable);
    }
}
