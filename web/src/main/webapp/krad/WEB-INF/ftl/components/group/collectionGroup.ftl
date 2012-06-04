<#--
    ~ Copyright 2006-2012 The Kuali Foundation
    ~
    ~ Licensed under the Educational Community License, Version 2.0 (the "License");
    ~ you may not use this file except in compliance with the License.
    ~ You may obtain a copy of the License at
    ~
    ~ http://www.opensource.org/licenses/ecl2.php
    ~
    ~ Unless required by applicable law or agreed to in writing, software
    ~ distributed under the License is distributed on an "AS IS" BASIS,
    ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    ~ See the License for the specific language governing permissions and
    ~ limitations under the License.
    -->

<@krad.groupWrap group=group>

    <#-- render collection quickfinder -->
    <@krad.template component=group.collectionLookup/>

    <#assign tmpItems=items!/>
    <#assign tmpManager=manager!/>
    <#assign tmpContainer=container!/>

    <#assign items=group.items/>
    <#assign manager=group.layoutManager/>
    <#assign container=group/>

    <#include "${group.layoutManager.template}" parse=true/>

    <#assign items=tmpItems/>
    <#assign manager=tmpManager/>
    <#assign container=tmpContainer/>

</@krad.groupWrap>
