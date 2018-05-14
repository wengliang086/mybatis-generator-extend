package com.my.testgen.dao.mybatis.impl;

import com.my.testgen.dao.UserLoginInfoDao;
import com.my.testgen.dao.mybatis.mapper.UserLoginInfoMapper;
import com.my.testgen.dao.mybatis.vo.UserLoginInfo;
import java.lang.Long;
import javax.annotation.Resource;
import org.springframework.stereotype.Repository;




@Repository
public class UserLoginInfoDaoImpl implements UserLoginInfoDao {
	
	@Resource
	private UserLoginInfoMapper userLoginInfoMapper;

	@Override
	public void save(UserLoginInfo userLoginInfo){
		userLoginInfoMapper.insert(userLoginInfo);
	}

	@Override
	public UserLoginInfo get(Long uid) {
		return userLoginInfoMapper.selectByPrimaryKey(uid);
	}

	@Override
	public void update(UserLoginInfo userLoginInfo) {
		userLoginInfoMapper.updateByPrimaryKey(userLoginInfo);
	}

	@Override
	public void delete(Long uid) {
		userLoginInfoMapper.deleteByPrimaryKey(uid);
	}
	
	 
	////*******自定义开始********//
	//**********自定义结束*****////
	
}
