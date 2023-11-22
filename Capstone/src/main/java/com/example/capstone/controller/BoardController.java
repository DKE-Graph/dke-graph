package com.example.capstone.controller;

import com.example.capstone.entity.Board;
import com.example.capstone.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class BoardController {
    @Autowired
    private BoardService boardService;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/board/C")
    public String Cboard(Model model, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, String searchKeyword) {

        Page<Board> list = null;

        if(searchKeyword != null) {
            list = boardService.boardSearchList(searchKeyword,pageable);
        }
        else {
            list = boardService.boardlist(pageable);
        }

        int nowPage = list.getPageable().getPageNumber() + 1;

        int startPage = Math.max(nowPage-4, 1);
        int endpage = Math.min(nowPage+5, list.getTotalPages());

        model.addAttribute("list", list);
        model.addAttribute("nowPage",nowPage);
        model.addAttribute("startPage",startPage);
        model.addAttribute("endPage",endpage);
        return "C"; //
    }

    @GetMapping("/board/JAVA")
    public String JAVAboard(Model model, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, String searchKeyword) {

        Page<Board> list = null;

        if(searchKeyword != null) {
            list = boardService.boardSearchList(searchKeyword,pageable);
        }
        else {
            list = boardService.boardlist(pageable);
        }

        int nowPage = list.getPageable().getPageNumber() + 1;

        int startPage = Math.max(nowPage-4, 1);
        int endpage = Math.min(nowPage+5, list.getTotalPages());

        model.addAttribute("list", list);
        model.addAttribute("nowPage",nowPage);
        model.addAttribute("startPage",startPage);
        model.addAttribute("endPage",endpage);
        return "JAVA"; //
    }

    @GetMapping("/board/PYTHON")
    public String PYTHONboard(Model model, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, String searchKeyword) {

        Page<Board> list = null;

        if(searchKeyword != null) {
            list = boardService.boardSearchList(searchKeyword,pageable);
        }
        else {
            list = boardService.boardlist(pageable);
        }

        int nowPage = list.getPageable().getPageNumber() + 1;

        int startPage = Math.max(nowPage-4, 1);
        int endpage = Math.min(nowPage+5, list.getTotalPages());

        model.addAttribute("list", list);
        model.addAttribute("nowPage",nowPage);
        model.addAttribute("startPage",startPage);
        model.addAttribute("endPage",endpage);
        return "PYTHON"; //
    }

    @GetMapping("/write")
    public String boardwriteForm() { return "write"; }
    @PostMapping("/write")
    public String boardwrite(Board board, Model model) {
        boardService.write(board);

        model.addAttribute("message", "글 작성이 완료되었습니다.");
        model.addAttribute("searchUrl", "/board/list");

        return "message";
    }

    // 취소 요청 시 list 페이지로 이동하도록 수정
    @GetMapping("/board/cancel")
    public String boardcancel() { return "redirect:/board/list"; }

    @GetMapping("/board/list")
    public String boardlist(Model model, @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, String searchKeyword) {
        Page<Board> list = null;

        if(searchKeyword != null) {
            list = boardService.boardSearchList(searchKeyword,pageable);
        }
        else {
            list = boardService.boardlist(pageable);
        }

        int nowPage = list.getPageable().getPageNumber() + 1;

        int startPage = Math.max(nowPage-4, 1);
        int endpage = Math.min(nowPage+5, list.getTotalPages());

        model.addAttribute("list", list);
        model.addAttribute("nowPage",nowPage);
        model.addAttribute("startPage",startPage);
        model.addAttribute("endPage",endpage);
        return "boardlist"; // boardlist -> C
    }

    @GetMapping("board/view")
    public String boardview(Model model, Long id) {
        model.addAttribute("board", boardService.boardview(id));
        return "boardview";
    }

    @PostMapping("/board/delete/{id}")
    public String boardDelete(@PathVariable("id") Long id, Board board, Model model, String password) {
        Board boardTemp2 = boardService.boardview(id);
        if (!boardTemp2.getPassword().equals(password)) {
            model.addAttribute("message", "비밀번호가 옳지 않습니다");
            model.addAttribute("searchUrl", "/board/list");
            return "message";
        } else {
            boardService.boardDelete(id);
            model.addAttribute("message", "글 삭제가 완료되었습니다");
            model.addAttribute("searchUrl", "/board/list");
            return "message";
        }
    }
    @GetMapping("/board/modify/{id}")
    public String boardModify(@PathVariable("id") Long id, Model model) {
        model.addAttribute("board", boardService.boardview(id));
        return "boardmodify";
    }

    @PostMapping("/board/update/{id}")
    public String boardUpdate(@PathVariable("id") Long id,Board board, Model model,String password) {
        Board boardTemp = boardService.boardview(id);
        if(!boardTemp.getPassword().equals(password)) {
            model.addAttribute("message", "비밀번호가 옳지 않습니다");
            model.addAttribute("searchUrl", "/board/list");
            return "message";
        }
        else {
            boardTemp.setTitle(board.getTitle()); // 수정한 내용을 board 자료형의 baordTemp 변수의 title 과 content로 설정
            boardTemp.setContent(board.getContent()); //

            boardService.write(boardTemp); // 설정한 boardTemp 내용을 다시 해당 게시물에 덮어쓰기

            model.addAttribute("message", "글 수정이 완료되었습니다");
            model.addAttribute("searchUrl", "/board/list");

            return "message";
        }
    }
    @RequestMapping("/board/test1")
    public String test1() {
        return "test1";
    }
}
