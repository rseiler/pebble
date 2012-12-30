/*******************************************************************************
 * This file is part of Pebble.
 * 
 * Copyright (c) 2012 Mitchell Bosecke.
 * 
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 
 * Unported License. To view a copy of this license, visit 
 * http://creativecommons.org/licenses/by-sa/3.0/
 ******************************************************************************/
package com.mitchellbosecke.pebble.extension;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.filter.Filter;
import com.mitchellbosecke.pebble.filter.FilterFunction;
import com.mitchellbosecke.pebble.node.expression.binary.NodeExpressionBinaryAnd;
import com.mitchellbosecke.pebble.node.expression.binary.NodeExpressionBinaryEqual;
import com.mitchellbosecke.pebble.node.expression.binary.NodeExpressionBinaryIs;
import com.mitchellbosecke.pebble.node.expression.binary.NodeExpressionBinaryIsNot;
import com.mitchellbosecke.pebble.node.expression.binary.NodeExpressionBinaryNotEqual;
import com.mitchellbosecke.pebble.node.expression.binary.NodeExpressionBinaryOr;
import com.mitchellbosecke.pebble.parser.Operator;
import com.mitchellbosecke.pebble.test.Test;
import com.mitchellbosecke.pebble.test.TestFunction;
import com.mitchellbosecke.pebble.tokenParser.BlockTokenParser;
import com.mitchellbosecke.pebble.tokenParser.ExtendsTokenParser;
import com.mitchellbosecke.pebble.tokenParser.ForTokenParser;
import com.mitchellbosecke.pebble.tokenParser.IfTokenParser;
import com.mitchellbosecke.pebble.tokenParser.ImportTokenParser;
import com.mitchellbosecke.pebble.tokenParser.IncludeTokenParser;
import com.mitchellbosecke.pebble.tokenParser.MacroTokenParser;
import com.mitchellbosecke.pebble.tokenParser.TokenParser;
import com.mitchellbosecke.pebble.utils.Command;

public class CoreExtension implements Extension {

	@Override
	public void initRuntime(PebbleEngine engine) {
	}

	@Override
	public List<TokenParser> getTokenParsers() {
		ArrayList<TokenParser> parsers = new ArrayList<>();
		parsers.add(new BlockTokenParser());
		parsers.add(new ExtendsTokenParser());
		parsers.add(new IfTokenParser());
		parsers.add(new ForTokenParser());
		parsers.add(new MacroTokenParser());
		parsers.add(new ImportTokenParser());
		parsers.add(new IncludeTokenParser());
		return parsers;
	}
	
	@Override
	public List<Operator> getUnaryOperators() {
		return null;
	}

	@Override
	public List<Operator> getBinaryOperators() {
		ArrayList<Operator> operators = new ArrayList<>();
		operators.add(new Operator("and", 15, NodeExpressionBinaryAnd.class, Operator.Associativity.LEFT));
		operators.add(new Operator("or", 10, NodeExpressionBinaryOr.class, Operator.Associativity.LEFT));
		operators.add(new Operator("==", 20, NodeExpressionBinaryEqual.class, Operator.Associativity.LEFT));
		operators.add(new Operator("!=", 20, NodeExpressionBinaryNotEqual.class, Operator.Associativity.LEFT));
		operators.add(new Operator("is", 100, NodeExpressionBinaryIs.class, Operator.Associativity.LEFT));
		operators.add(new Operator("is not", 100, NodeExpressionBinaryIsNot.class, Operator.Associativity.LEFT));
		return operators;
	}

	@Override
	public List<Filter> getFilters() {
		ArrayList<Filter> filters = new ArrayList<>();
		filters.add(new FilterFunction("lower", lowerFilter));
		filters.add(new FilterFunction("upper", upperFilter));
		filters.add(new FilterFunction("date", dateFilter));
		filters.add(new FilterFunction("url_encode", urlEncoderFilter));
		filters.add(new FilterFunction("format", formatFilter));
		filters.add(new FilterFunction("number_format", numberFilter));
		filters.add(new FilterFunction("abbreviate", abbreviateFilter));
		filters.add(new FilterFunction("capitalize", capitalizeFilter));
		filters.add(new FilterFunction("trim", trimFilter));
		filters.add(new FilterFunction("json_encode", jsonEncodeFilter));
		filters.add(new FilterFunction("default", defaultFilter));
		return filters;
	}
	
	@Override
	public List<Test> getTests(){
		ArrayList<Test> tests = new ArrayList<>();
		tests.add(new TestFunction("even", evenTest));
		tests.add(new TestFunction("odd", oddTest));
		tests.add(new TestFunction("null", nullTest));
		tests.add(new TestFunction("empty", emptyTest));
		tests.add(new TestFunction("iterable", iterableTest));
		return tests;
	}

	private Command<Object, List<Object>> lowerFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {
			// first argument should be a string
			String arg = (String) data.get(0);
			return arg.toLowerCase();
		}
	};

	private Command<Object, List<Object>> upperFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {
			// first argument should be a string
			String arg = (String) data.get(0);
			return arg.toUpperCase();
		}
	};

	private Command<Object, List<Object>> urlEncoderFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {
			// first argument should be a string
			String arg = (String) data.get(0);
			try {
				arg = URLEncoder.encode(arg, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
			return arg;
		}
	};

	private Command<Object, List<Object>> formatFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {
			// first argument should be a string
			String arg = (String) data.get(0);

			Object[] formatArgs = data.subList(1, data.size()).toArray();

			return String.format(arg, formatArgs);
		}
	};

	private Command<Object, List<Object>> dateFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {

			Date arg = null;

			if (data.size() == 2) {
				arg = (Date) data.get(0);
			} else if (data.size() == 3) {
				// second argument is the format of the existing date
				DateFormat originalFormat = new SimpleDateFormat((String) data.get(1));

				try {
					arg = originalFormat.parse((String) data.get(0));
				} catch (ParseException e) {
					// TODO: figure out what to do here
				}
			}

			// last argument is the intended format
			DateFormat format = new SimpleDateFormat((String) data.get(data.size() - 1));

			return format.format(arg);
		}
	};

	private Command<Object, List<Object>> numberFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {

			Double number = (Double) data.get(0);

			Format format = new DecimalFormat((String) data.get(1));

			return format.format(number);
		}
	};

	private Command<Object, List<Object>> abbreviateFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {

			String str = (String) data.get(0);

			int maxWidth = (Integer) data.get(1);

			return StringUtils.abbreviate(str, maxWidth);
		}
	};

	private Command<Object, List<Object>> capitalizeFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {

			String str = (String) data.get(0);
			return StringUtils.capitalize(str);
		}
	};

	private Command<Object, List<Object>> trimFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {

			String str = (String) data.get(0);
			return str.trim();
		}
	};

	private Command<Object, List<Object>> jsonEncodeFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {

			Object obj = data.get(0);

			ObjectMapper mapper = new ObjectMapper();

			String json = null;
			try {
				json = mapper.writeValueAsString(obj);
			} catch (JsonProcessingException e) {
			}
			return json;
		}
	};


	private Command<Object, List<Object>> defaultFilter = new Command<Object, List<Object>>() {
		@Override
		public Object execute(List<Object> data) {

			Object obj = data.get(0);

			Object defaultObj = data.get(1);
			
			if(emptyTest.execute(data)){
				return defaultObj;
			}
			return obj;
		}
	};
	
	
	private Command<Boolean, List<Object>> evenTest = new Command<Boolean, List<Object>>() {
		@Override
		public Boolean execute(List<Object> data) {

			Integer obj = (Integer) data.get(0);
			
			return (obj % 2 == 0);
		}
	};
	
	private Command<Boolean, List<Object>> oddTest = new Command<Boolean, List<Object>>() {
		@Override
		public Boolean execute(List<Object> data) {

			return evenTest.execute(data) == false;
		}
	};
	
	private Command<Boolean, List<Object>> nullTest = new Command<Boolean, List<Object>>() {
		@Override
		public Boolean execute(List<Object> data) {
			return (data.get(0) == null);
		}
	};
	
	private Command<Boolean, List<Object>> emptyTest = new Command<Boolean, List<Object>>() {
		@Override
		public Boolean execute(List<Object> data) {
			Object obj = data.get(0);
			boolean isEmpty = obj == null;
			
			if(!isEmpty && obj instanceof String){
				isEmpty = StringUtils.isBlank(((String)obj));
			}
			
			if(!isEmpty && obj instanceof Collection){
				isEmpty = ((Collection<?>)obj).isEmpty();
			}
			
			if(!isEmpty && obj instanceof Map){
				isEmpty = ((Map<?,?>)obj).isEmpty();
			}
			
			return isEmpty;
		}
	};

	private Command<Boolean, List<Object>> iterableTest = new Command<Boolean, List<Object>>() {
		@Override
		public Boolean execute(List<Object> data) {
			Object obj = data.get(0);
			
			return obj instanceof Iterable;
		}
	};
	
}
