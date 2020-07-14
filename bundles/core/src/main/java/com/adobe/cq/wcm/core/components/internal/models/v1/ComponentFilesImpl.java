/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.internal.models.v1;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.wcm.core.components.internal.Utils;
import com.adobe.cq.wcm.core.components.models.ComponentFiles;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = ComponentFiles.class
)
public class ComponentFilesImpl implements ComponentFiles {

    @Self
    SlingHttpServletRequest request;

    @Inject
    @Named(OPTION_RESOURCE_TYPES)
    Collection<String> resourceTypes;

    @Inject
    @Named(OPTION_FILTER_REGEX)
    String filterRegex;

    @Inject
    @Named(OPTION_INHERITED)
    @Default(booleanValues = OPTION_INHERITED_DEFAULT)
    boolean inherited;

    @OSGiService
    ResourceResolverFactory resolverFactory;

    private Pattern pattern;
    private List<String> paths;

    @PostConstruct
    public void init() {
        pattern = Pattern.compile(filterRegex);
    }

    @Override
    public List<String> getPaths() {
        if (paths == null) {
            paths = new LinkedList<>();

            Set<String> seenResourceTypes = new HashSet<>();
            for (String resourceType : resourceTypes) {
                addPaths(resourceType, paths, seenResourceTypes);
            }
        }
        return paths;
    }

    /**
     * Adds file paths to a given collection, based on a resource type, filtered by the defined RegEx pattern.
     *
     * @param resourceType - the resource type of the component to look into for files matching the defined pattern
     * @param paths - the given collection of file paths
     * @param seenResourceTypes - a set of resource types that were previously searched into, to avoid inheritance loops
     */
    private void addPaths(String resourceType, Collection<String> paths, Set<String> seenResourceTypes) {
        if (!seenResourceTypes.contains(resourceType)) {
            Resource resource = getResource(resourceType);
            if (resource != null) {
                for (Resource child : resource.getChildren()) {
                    if (pattern.matcher(child.getName()).matches()) {
                        paths.add(child.getPath());
                    }
                }
                if (inherited) {
                    addPaths(resource.getResourceSuperType(), paths, seenResourceTypes);
                }
            }
        }
    }

    /**
     * Gets the component resource for a given path
     *
     * @param path - the path
     *
     * @return the corresponding resource
     */
    private Resource getResource(String path) {
        if (path == null) {
            return null;
        }
        ResourceResolver resolver = Utils.getComponentsResolver(resolverFactory);
        if (resolver == null) {
            resolver = request.getResourceResolver();
        }
        return resolver.getResource(path);
    }
}