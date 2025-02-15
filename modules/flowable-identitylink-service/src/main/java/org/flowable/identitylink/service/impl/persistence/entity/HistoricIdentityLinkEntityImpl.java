/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.identitylink.service.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Frederik Heremans
 */
public class HistoricIdentityLinkEntityImpl extends AbstractIdentityLinkServiceNoRevisionEntity implements HistoricIdentityLinkEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String type;
    protected String userId;
    protected String groupId;
    protected String taskId;
    protected String processInstanceId;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected Date createTime;

    public HistoricIdentityLinkEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        // Historic identity link is immutable
        return HistoricIdentityLinkEntityImpl.class;
    }

    @Override
    public boolean isUser() {
        return userId != null;
    }

    @Override
    public boolean isGroup() {
        return groupId != null;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        if (this.groupId != null && userId != null) {
            throw new FlowableException("Cannot assign a userId to a task assignment that already has a groupId");
        }
        this.userId = userId;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(String groupId) {
        if (this.userId != null && groupId != null) {
            throw new FlowableException("Cannot assign a groupId to a task assignment that already has a userId");
        }
        this.groupId = groupId;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    
    @Override
    public String getScopeId() {
        return this.scopeId;
    }
    
    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }
    
    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    @Override
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    @Override
    public String getScopeType() {
        return this.scopeType;
    }
    
    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getScopeDefinitionId() {
        return this.scopeDefinitionId;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IdentityLinkEntity[id=").append(id);
        sb.append(", type=").append(type);
        if (userId != null) {
            sb.append(", userId=").append(userId);
        }
        if (groupId != null) {
            sb.append(", groupId=").append(groupId);
        }
        if (taskId != null) {
            sb.append(", taskId=").append(taskId);
        }
        if (processInstanceId != null) {
            sb.append(", processInstanceId=").append(processInstanceId);
        }
        if (scopeId != null) {
            sb.append(", scopeId=").append(scopeId);
        }
        if (scopeType != null) {
            sb.append(", scopeType=").append(scopeType);
        }
        if (scopeDefinitionId != null) {
            sb.append(", scopeDefinitionId=").append(scopeDefinitionId);
        }
        sb.append("]");
        return sb.toString();
    }
}
