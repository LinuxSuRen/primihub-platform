package com.primihub.biz.service.sys;

import com.primihub.biz.config.base.BaseConfiguration;
import com.primihub.biz.config.base.OrganConfiguration;
import com.primihub.biz.constant.RedisKeyConstant;
import com.primihub.biz.constant.SysConstant;
import com.primihub.biz.entity.base.BaseResultEntity;
import com.primihub.biz.entity.base.BaseResultEnum;
import com.primihub.biz.entity.base.PageParam;
import com.primihub.biz.entity.sys.param.*;
import com.primihub.biz.entity.sys.po.SysRole;
import com.primihub.biz.entity.sys.po.SysUr;
import com.primihub.biz.entity.sys.po.SysUser;
import com.primihub.biz.entity.sys.vo.SysAuthNodeVO;
import com.primihub.biz.entity.sys.vo.SysUserListVO;
import com.primihub.biz.repository.primarydb.sys.SysUserPrimarydbRepository;
import com.primihub.biz.repository.primaryredis.sys.SysCommonPrimaryRedisRepository;
import com.primihub.biz.repository.primaryredis.sys.SysUserPrimaryRedisRepository;
import com.primihub.biz.repository.secondarydb.sys.SysRoleSecondarydbRepository;
import com.primihub.biz.repository.secondarydb.sys.SysUserSecondarydbRepository;
import com.primihub.biz.tool.PlatformHelper;
import com.primihub.biz.util.crypt.CryptUtil;
import com.primihub.biz.util.crypt.SignUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SysUserService {

    @Autowired
    private SysUserPrimarydbRepository sysUserPrimarydbRepository;
    @Autowired
    private SysUserSecondarydbRepository sysUserSecondarydbRepository;
    @Autowired
    private SysRoleSecondarydbRepository sysRoleSecondarydbRepository;
    @Autowired
    private SysCommonPrimaryRedisRepository sysCommonPrimaryRedisRepository;
    @Autowired
    private SysUserPrimaryRedisRepository sysUserPrimaryRedisRepository;
    @Autowired
    private SysAuthService sysAuthService;
    @Autowired
    private BaseConfiguration baseConfiguration;
    @Autowired
    private OrganConfiguration organConfiguration;
    @Resource(name="soaRestTemplate")
    private RestTemplate restTemplate;

    public BaseResultEntity login(LoginParam loginParam){
        String privateKey=sysCommonPrimaryRedisRepository.getRsaKey(loginParam.getValidateKeyName());
        if(privateKey==null)
            return BaseResultEntity.failure(BaseResultEnum.VALIDATE_KEY_INVALIDATION);
        SysUser sysUser=sysUserSecondarydbRepository.selectUserByUserAccount(loginParam.getUserAccount());
        if(sysUser==null||sysUser.getUserId()==null)
            return BaseResultEntity.failure(BaseResultEnum.ACCOUNT_NOT_FOUND);

        String userPassword;
        try {
            userPassword=CryptUtil.decryptRsaWithPrivateKey(loginParam.getUserPassword(),privateKey);
        } catch (Exception e) {
            return BaseResultEntity.failure(BaseResultEnum.FAILURE,"解密失败");
        } 
        StringBuffer sb=new StringBuffer().append(baseConfiguration.getDefaultPasswordVector()).append(userPassword);
        String signPassword=SignUtil.getMD5ValueLowerCaseByDefaultEncode(sb.toString());
        if(!signPassword.equals(sysUser.getUserPassword()))
            return BaseResultEntity.failure(BaseResultEnum.PASSWORD_NOT_CORRECT);

        Set<Long> roleIdSet=Stream.of(sysUser.getRoleIdList().split(",")).filter(item->!item.equals(""))
                .map(item->(Long.parseLong(item))).collect(Collectors.toSet());
        Set<Long> authIdList=sysRoleSecondarydbRepository.selectRaByBatchRoleId(roleIdSet);
        List<SysAuthNodeVO> roleAuthRootList=sysAuthService.getSysAuthForBfs();
        List<SysAuthNodeVO> grantAuthRootList=roleAuthRootList.stream().filter(item->authIdList.contains(item.getAuthId()))
                .map(item->{item.setIsGrant(1); return item;}).collect(Collectors.toList());


        List<SysRole> roleList=roleIdSet.size()==0?new ArrayList<>():sysRoleSecondarydbRepository.selectSysRoleByBatchRoleId(roleIdSet);
        Map<Long,String> roleMap=roleList.stream().collect(Collectors.toMap(SysRole::getRoleId,SysRole::getRoleName,(x,y)->x));

        SysUserListVO sysUserListVO=new SysUserListVO();
        BeanUtils.copyProperties(sysUser,sysUserListVO);
        String authIdListStr=authIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
        sysUserListVO.setAuthIdList(authIdListStr);

        String roleIdListDesc = formIdDesc(roleMap, sysUserListVO.getRoleIdList());
        sysUserListVO.setRoleIdListDesc(roleIdListDesc);
        sysUserListVO.setOrganIdListDesc(organConfiguration.getSysLocalOrganName());
        sysUserListVO.setOrganIdList(organConfiguration.getSysLocalOrganId());

        Date date=new Date();

        String token= PlatformHelper.generateOwnToken(SysConstant.SYS_USER_TOKEN_PREFIX,sysUser.getUserId(),date);

        sysUserPrimaryRedisRepository.updateUserLoginStatus(token,sysUserListVO);

        Map map=new HashMap<>();
        map.put("sysUser",sysUserListVO);
        map.put("grantAuthRootList",grantAuthRootList);
        map.put("token",token);
        return BaseResultEntity.success(map);
    }

    public BaseResultEntity logout(String token,Long userId){
        if(token!=null&&token.equals("")&&userId==null) {
            sysUserPrimaryRedisRepository.deleteUserLoginStatus(token, userId);
        }
        return BaseResultEntity.success();
    }

    public BaseResultEntity saveOrUpdateUser(SaveOrUpdateUserParam saveOrUpdateUserParam){
        Long userId=saveOrUpdateUserParam.getUserId();
        SysUser sysUser;
        if(userId==null){
            if(saveOrUpdateUserParam.getUserAccount()==null||saveOrUpdateUserParam.getUserAccount().trim().equals(""))
                return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"userAccount");
            if(saveOrUpdateUserParam.getUserName()==null||saveOrUpdateUserParam.getUserName().trim().equals(""))
                return BaseResultEntity.failure(BaseResultEnum.LACK_OF_PARAM,"userName");
            if(saveOrUpdateUserParam.getIsForbid()==null)
                saveOrUpdateUserParam.setIsForbid(0);
            if(sysUserSecondarydbRepository.isExistUserAccount(saveOrUpdateUserParam.getUserAccount()))
                return BaseResultEntity.failure(BaseResultEnum.NON_REPEATABLE,"账户名称");

            sysUser = new SysUser();
            BeanUtils.copyProperties(saveOrUpdateUserParam,sysUser);
            String password=saveOrUpdateUserParam.getPassword()!=null&&!saveOrUpdateUserParam.getPassword().equals("")?saveOrUpdateUserParam.getPassword():baseConfiguration.getDefaultPassword();
            StringBuffer sb=new StringBuffer().append(baseConfiguration.getDefaultPasswordVector()).append(password);
            sysUser.setUserPassword(SignUtil.getMD5ValueLowerCaseByDefaultEncode(sb.toString()));
            sysUser.setRoleIdList("");
            sysUser.setIsForbid(saveOrUpdateUserParam.getIsForbid());
            sysUser.setIsEditable(1);
            sysUser.setIsDel(0);
            sysUserPrimarydbRepository.insertSysUser(sysUser);
            userId=sysUser.getUserId();
        }else{
            sysUser=sysUserSecondarydbRepository.selectSysUserByUserId(userId);
            if(sysUser==null||sysUser.getUserId()==null)
                return BaseResultEntity.failure(BaseResultEnum.CAN_NOT_ALTER,"不存在该数据");
            if(sysUser.getIsEditable().equals(0))
                return BaseResultEntity.failure(BaseResultEnum.CAN_NOT_ALTER,"该记录是不可编辑状态");
            if((saveOrUpdateUserParam.getUserName()!=null&&!saveOrUpdateUserParam.getUserName().trim().equals(""))
                ||(saveOrUpdateUserParam.getIsForbid()!=null)){
                Map paramMap=new HashMap(){
                    {
                        put("userId",saveOrUpdateUserParam.getUserId());
                        put("userName",saveOrUpdateUserParam.getUserName());
                        put("isForbid",saveOrUpdateUserParam.getIsForbid());
                    }
                };
                sysUserPrimarydbRepository.updateSysUserExplicit(paramMap);
                sysUserPrimaryRedisRepository.deleteUserLoginStatus(saveOrUpdateUserParam.getUserId());
            }
        }

        boolean roleFlag=false;
        if(saveOrUpdateUserParam.getRoleIdList()!=null&&saveOrUpdateUserParam.getRoleIdList().length!=0){
            roleFlag=true;
//            sysUserPrimarydbRepository.deleteSysUrBatch(saveOrUpdateUserParam.getRoleIdList(),userId);
            sysUserPrimarydbRepository.deleteSysUrBatch(userId);
            List<SysUr> urList=new ArrayList<>();
            for(Long roleId:saveOrUpdateUserParam.getRoleIdList()){
                SysUr sysUr=new SysUr();
                sysUr.setRoleId(roleId);
                sysUr.setUserId(userId);
                sysUr.setIsDel(0);
                urList.add(sysUr);
            }
            sysUserPrimarydbRepository.insertSysUrBatch(urList);
        }


        String roleIdListStr=roleFlag?Stream.of(saveOrUpdateUserParam.getRoleIdList()).map(item->(String.valueOf(item))).collect(Collectors.joining(",")):"";
        if(!roleIdListStr.equals("")){
            Map paramMap=new HashMap(){
                {
                    put("userId",sysUser.getUserId());
                    put("roleIdList",roleIdListStr);
                }
            };
            sysUserPrimarydbRepository.updateSysUserExplicit(paramMap);
            sysUserPrimaryRedisRepository.deleteUserLoginStatus(sysUser.getUserId());
        }

        sysUser.setUserPassword("");
        Map map=new HashMap<>();
        map.put("sysUser",sysUser);
        return BaseResultEntity.success(map);
    }

    public BaseResultEntity deleteSysUser(Long userId){
        SysUser sysUser=sysUserSecondarydbRepository.selectSysUserByUserId(userId);
        if(sysUser==null||sysUser.getUserId()==null)
            return BaseResultEntity.failure(BaseResultEnum.CAN_NOT_DELETE,"不存在该数据");
        if(sysUser.getIsEditable().equals(0))
            return BaseResultEntity.failure(BaseResultEnum.CAN_NOT_DELETE,"该记录是不可编辑状态");
        sysUserPrimarydbRepository.updateSysUserDelStatus(userId);
        sysUserPrimaryRedisRepository.deleteUserLoginStatus(userId);
        sysUserPrimarydbRepository.deleteSysUrBatch(userId);
        return BaseResultEntity.success();
    }

    public BaseResultEntity findUserPage(FindUserPageParam findUserPageParam,Integer pageNum,Integer pageSize){
        PageParam pageParam=new PageParam(pageNum,pageSize);
        Map paramMap=new HashMap(){
            {
                put("userName",findUserPageParam.getUserName());
                put("roleId",findUserPageParam.getRoleId());
                put("pageIndex",pageParam.getPageIndex());
                put("pageSize",pageParam.getPageSize()+1);
            }
        };
        List<SysUserListVO> sysUserList =sysUserSecondarydbRepository.selectSysUserListByParam(paramMap);
        Long count=sysUserSecondarydbRepository.selectSysUserListCountByParam(paramMap);
        Set<Long> roleIdSet=new HashSet<>();
        Set<Long> organIdSet=new HashSet<>();
        for(SysUserListVO sysUserListVO:sysUserList){
            roleIdSet.addAll(Stream.of(sysUserListVO.getRoleIdList().split(",")).filter(item->!item.equals(""))
                    .map(item->(Long.parseLong(item))).collect(Collectors.toSet()));
        }
        List<SysRole> roleList=roleIdSet.size()==0?new ArrayList<>():sysRoleSecondarydbRepository.selectSysRoleByBatchRoleId(roleIdSet);
        Map<Long,String> roleMap=roleList.stream().collect(Collectors.toMap(SysRole::getRoleId,SysRole::getRoleName,(x,y)->x));
        for(SysUserListVO sysUserListVO:sysUserList){
            String roleIdListDesc = formIdDesc(roleMap, sysUserListVO.getRoleIdList());
            sysUserListVO.setRoleIdListDesc(roleIdListDesc);
        }
        pageParam.isLoadMore(sysUserList);
        pageParam.initItemTotalCount(count);
        Map map=new HashMap<>();
        map.put("sysUserList",sysUserList);
        map.put("pageParam",pageParam);
        return BaseResultEntity.success(map);
    }

    private String formIdDesc(Map<Long, String> roleMap, String idListStr) {
        StringBuilder sb=new StringBuilder();
        if(idListStr!=null&&!idListStr.trim().equals("")) {
            String[] idStrArray = idListStr.split(",");
            for (String idStr:idStrArray){
                if(idStr!=null&&!idStr.trim().equals("")){
                    Long currentId=Long.parseLong(idStr);
                    String currentName= roleMap.get(currentId);
                    if(currentName!=null)
                        sb.append(currentName).append(",");
                }
            }
        }
        if(sb.length()>0) sb.setLength(sb.length()-1);
        return sb.toString();
    }

    public BaseResultEntity initPassword(Long userId){
        updatePassword(userId,baseConfiguration.getDefaultPassword());
        return BaseResultEntity.success();
    }

    public Map<Long, SysUser> getSysUserMap(Set<Long> userIdSet){
        if(userIdSet==null||userIdSet.size()==0){
            return new HashMap<>();
        }
        List<SysUser> sysUsers = this.sysUserSecondarydbRepository.selectSysUserByUserIdSet(userIdSet);
        if (sysUsers.size()>0){
            return sysUsers.stream().collect(Collectors.toMap(SysUser::getUserId, Function.identity(),(key1,key2)->key2));
        }
        return new HashMap<>();
    }

    public SysUser getSysUserById(Long userid){
        if (userid == null || userid.compareTo(0L) == 0){
            return null;
        }
        return this.sysUserSecondarydbRepository.selectSysUserByUserId(userid);
    }

    public BaseResultEntity updatePassword(Long userId, String password,String validateKeyName) {
        String privateKey=sysCommonPrimaryRedisRepository.getRsaKey(validateKeyName);
        if(privateKey==null)
            return BaseResultEntity.failure(BaseResultEnum.VALIDATE_KEY_INVALIDATION);
        String oldAndNewPassword;
        try {
            oldAndNewPassword=CryptUtil.decryptRsaWithPrivateKey(password,privateKey);
        } catch (Exception e) {
            return BaseResultEntity.failure(BaseResultEnum.FAILURE,"解密失败");
        }
        String[] passwordArray = oldAndNewPassword.split(",");
        if (passwordArray.length!=2)
            return BaseResultEntity.failure(BaseResultEnum.PARAM_INVALIDATION,"password");
        SysUser sysUser=sysUserSecondarydbRepository.selectSysUserContainPassByUserId(userId);
        if(sysUser==null||sysUser.getUserId()==null)
            return BaseResultEntity.failure(BaseResultEnum.ACCOUNT_NOT_FOUND);
        String oldPassword = passwordArray[0];
        StringBuffer sb=new StringBuffer().append(baseConfiguration.getDefaultPasswordVector()).append(oldPassword);
        String signPassword=SignUtil.getMD5ValueLowerCaseByDefaultEncode(sb.toString());
        if(!signPassword.equals(sysUser.getUserPassword()))
            return BaseResultEntity.failure(BaseResultEnum.OLD_PASSWORD_NOT_CORRECT);
        String newPassword = passwordArray[1];
        updatePassword(userId,newPassword);
        return BaseResultEntity.success();
    }

    public void updatePassword(Long userId, String password){
        StringBuffer sb=new StringBuffer().append(baseConfiguration.getDefaultPasswordVector()).append(password);
        String userPassword=SignUtil.getMD5ValueLowerCaseByDefaultEncode(sb.toString());
        Map paramMap=new HashMap(){
            {
                put("userId",userId);
                put("userPassword",userPassword);
            }
        };
        sysUserPrimarydbRepository.updateSysUserExplicit(paramMap);
    }

    public BaseResultEntity findUserByAccount(String userAccount){
        SysUser sysUser=sysUserSecondarydbRepository.selectUserByUserAccount(userAccount);
        Map map=new HashMap<>();
        map.put("sysUser",sysUser);
        return BaseResultEntity.success(map);
    }

    public BaseResultEntity sendVerificationCode(SendVerificationCodeParam sendVerificationCodeParam){
        String verificationCode=sendVerificationCodeParam.getCellphone()!=null&&!sendVerificationCodeParam.getCellphone().equals("")?String.valueOf(RandomUtils.nextInt(100000,999999)): RandomStringUtils.randomAlphanumeric(8);
        String userAccount=sendVerificationCodeParam.getCellphone()!=null&&!sendVerificationCodeParam.getCellphone().equals("")?sendVerificationCodeParam.getCellphone():sendVerificationCodeParam.getEmail();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap map = new LinkedMultiValueMap<>();
        if(sendVerificationCodeParam.getCellphone()!=null&&!sendVerificationCodeParam.getCellphone().equals(""))
            map.put("cellphone", new ArrayList(){{add(sendVerificationCodeParam.getCellphone());}});
        if(sendVerificationCodeParam.getEmail()!=null&&!sendVerificationCodeParam.getEmail().equals(""))
            map.put("email", new ArrayList(){{add(sendVerificationCodeParam.getEmail());}});
        map.put("verify_code", new ArrayList(){{add(verificationCode);}});
        HttpEntity<HashMap<String, Object>> request = new HttpEntity(map, headers);
        BaseResultEntity baseResultEntity=restTemplate.postForObject(baseConfiguration.getPrimihubOfficalService()+"/expericence/send_verify_code",request, BaseResultEntity.class);
        if(baseResultEntity.getCode().equals(-7))
            return BaseResultEntity.failure(BaseResultEnum.NOT_IN_THE_WHITE_LIST);
        else if(baseResultEntity.getCode().equals(-6))
            return BaseResultEntity.failure(BaseResultEnum.FIVE_MINUTES_LATER);
        else if(baseResultEntity.getCode().equals(-5))
            return BaseResultEntity.failure(BaseResultEnum.OUTNUMBER);
        else if(baseResultEntity.getCode().equals(-4))
            return BaseResultEntity.failure(BaseResultEnum.SMS_FAILURE);
        else if(baseResultEntity.getCode().equals(-8))
            return BaseResultEntity.failure(BaseResultEnum.ONE_MINUTES_LATER);
        else if(baseResultEntity.getCode().equals(-1))
            return BaseResultEntity.failure(BaseResultEnum.FAILURE,baseResultEntity.getMsg());

        String key=RedisKeyConstant.VERIFICATION_CODE_TYPE_KEY
                .replace("<code_type>",sendVerificationCodeParam.getCodeType().toString())
                .replace("<code_key>",userAccount);
        sysCommonPrimaryRedisRepository.setKeyWithExpire(key,verificationCode,5L, TimeUnit.MINUTES);

        return BaseResultEntity.success();
    }

    public boolean validateVerificationCode(Integer codeType,String codeKey,String verificationCode){
        String key=RedisKeyConstant.VERIFICATION_CODE_TYPE_KEY
                .replace("<code_type>",codeType.toString())
                .replace("<code_key>",codeKey);
        String result=sysCommonPrimaryRedisRepository.getKey(key);
        if(result==null||result.equals(""))
            return false;
        if(!result.equals(verificationCode))
            return false;
        return true;
    }

    public BaseResultEntity forgetPassword(ForgetPasswordParam forgetPasswordParam){
        SysUser sysUser=sysUserSecondarydbRepository.selectUserByUserAccount(forgetPasswordParam.getUserAccount());
        if(sysUser==null||sysUser.getUserId()==null)
            return BaseResultEntity.failure(BaseResultEnum.ACCOUNT_NOT_FOUND);
        updatePassword(sysUser.getUserId(),forgetPasswordParam.getPassword());
        return BaseResultEntity.success();
    }
}
