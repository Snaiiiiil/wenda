package com.nowcoder.wenda.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.text.html.ObjectView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nowcoder.wenda.model.Comment;
import com.nowcoder.wenda.model.EntityType;
import com.nowcoder.wenda.model.HostHolder;
import com.nowcoder.wenda.model.Question;
import com.nowcoder.wenda.model.User;
import com.nowcoder.wenda.model.ViewObject;
import com.nowcoder.wenda.service.CommentService;
import com.nowcoder.wenda.service.LikeService;
import com.nowcoder.wenda.service.QuestionService;
import com.nowcoder.wenda.service.UserService;
import com.nowcoder.wenda.util.WendaUtil;

@Controller
public class QuestionController {
	private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

	@Autowired
	HostHolder hostHolder;
	@Autowired
	QuestionService questionService;
	@Autowired
	UserService userService;
	@Autowired
	LikeService likeService;
	@Autowired
	CommentService commentService;

	@RequestMapping(path = "/question/add", method = { RequestMethod.POST })
	@ResponseBody
	public String addQuestion(@RequestParam("title") String title, @RequestParam("content") String content) {
		try {
			Question question = new Question();
			question.setTitle(title);
			question.setContent(content);
			question.setCreatedDate(new Date());
			question.setCommentCount(0);
			if (hostHolder.getUser() == null) {
				// question.setUserId(WendaUtil.ANONYMOUS_USERID);
				// 当前无用户时，提交问题后，会返回code999， 在popadd.js文件中将会跳转到reglogin页面
				return WendaUtil.getJSONString(999);
			} else
				question.setUserId(hostHolder.getUser().getId());
			if (questionService.addQuestion(question) > 0) {
				return WendaUtil.getJSONString(0);
			}
		} catch (Exception e) {
			// 创建问题失败的原因是执行过程中的问题
			logger.error("增加题目失败 ", e.getMessage());
		}
		// 创建问题失败的原因是插入语句有问题
		return WendaUtil.getJSONString(1, "失败");
	}

	@RequestMapping(path = "/question/{qid}", method = { RequestMethod.GET })
	public String questionDetail(Model model, @PathVariable("qid") int qid) {
		Question question = questionService.selectById(qid);
		model.addAttribute("question", question);

		// 获取评论和评论的用户
		List<Comment> commentList = commentService.getCommentByEntity(question.getId(), EntityType.ENTITY_QUESTION, 0, 0);
		List<ViewObject> comments = new ArrayList<>();
		for (Comment comment : commentList) {
			ViewObject viewObject = new ViewObject();
			long likeCount = likeService.getLikeCount(EntityType.ENTITY_COMMENT, comment.getId());
			viewObject.set("likeCount", likeCount);
			if (hostHolder.getUser() == null)
				viewObject.set("liked", 0);
			else {
				int likeStatus = likeService.getlikeStatus(hostHolder.getUser().getId(), EntityType.ENTITY_COMMENT,
						comment.getId());
				viewObject.set("liked", likeStatus);
			}
			viewObject.set("comment", comment);
			viewObject.set("user", userService.getUser(comment.getUserId()));
			comments.add(viewObject);
		}
		model.addAttribute("comments", comments);
		return "detail";
	}

}
