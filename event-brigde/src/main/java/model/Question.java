package model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class Question {
	private String text;
	private String img;
	private List<Answer> ans;
}
