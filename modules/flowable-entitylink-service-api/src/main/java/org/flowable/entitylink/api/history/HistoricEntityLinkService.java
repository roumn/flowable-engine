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
package org.flowable.entitylink.api.history;

import java.util.Collection;
import java.util.List;

import org.flowable.entitylink.api.InternalEntityLinkQuery;

/**
 * Service which provides access to historic entity links.
 * 
 * @author Tijs Rademakers
 */
public interface HistoricEntityLinkService {
    
    HistoricEntityLink getHistoricEntityLink(String id);
    
    default List<HistoricEntityLink> findHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType, String linkType) {
        return createInternalHistoricEntityLinkQuery()
                .scopeId(scopeId)
                .scopeType(scopeType)
                .linkType(linkType)
                .list();
    }

    List<HistoricEntityLink> findHistoricEntityLinksWithSameRootScopeForScopeIdAndScopeType(String scopeId, String scopeType, String linkType);
    
    List<HistoricEntityLink> findHistoricEntityLinksWithSameRootScopeForScopeIdsAndScopeType(Collection<String> scopeIds, String scopeType, String linkType);

    default List<HistoricEntityLink> findHistoricEntityLinksByReferenceScopeIdAndType(String referenceScopeId, String scopeType, String linkType) {
        return createInternalHistoricEntityLinkQuery()
                .referenceScopeId(referenceScopeId)
                .referenceScopeType(scopeType)
                .linkType(linkType)
                .list();
    }

    default List<HistoricEntityLink> findHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType, String linkType) {
        return createInternalHistoricEntityLinkQuery()
                .scopeDefinitionId(scopeDefinitionId)
                .scopeType(scopeType)
                .linkType(linkType)
                .list();
    }

    InternalEntityLinkQuery<HistoricEntityLink> createInternalHistoricEntityLinkQuery();

    HistoricEntityLink createHistoricEntityLink();
    
    void insertHistoricEntityLink(HistoricEntityLink entityLink, boolean fireCreateEvent);
    
    void deleteHistoricEntityLink(String id);
    
    void deleteHistoricEntityLink(HistoricEntityLink entityLink);
    
    void deleteHistoricEntityLinksByScopeIdAndScopeType(String scopeId, String scopeType);
    
    void deleteHistoricEntityLinksByScopeDefinitionIdAndScopeType(String scopeDefinitionId, String scopeType);
    
    void bulkDeleteHistoricEntityLinksForScopeTypeAndScopeIds(String scopeType, Collection<String> scopeIds);
    
    void deleteHistoricEntityLinksForNonExistingProcessInstances();
    
    void deleteHistoricEntityLinksForNonExistingCaseInstances();
}
