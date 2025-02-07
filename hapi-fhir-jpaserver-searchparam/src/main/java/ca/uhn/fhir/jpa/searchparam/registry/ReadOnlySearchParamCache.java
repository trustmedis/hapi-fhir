package ca.uhn.fhir.jpa.searchparam.registry;

/*-
 * #%L
 * HAPI FHIR Search Parameters
 * %%
 * Copyright (C) 2014 - 2021 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 * #L%
 */

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.ClasspathUtil;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ReadOnlySearchParamCache {

	// resourceName -> searchParamName -> searchparam
	protected final Map<String, Map<String, RuntimeSearchParam>> myResourceNameToSpNameToSp;
	protected final Map<String, RuntimeSearchParam> myUrlToParam;

	/**
	 * Constructor
	 */
	ReadOnlySearchParamCache() {
		myResourceNameToSpNameToSp = new HashMap<>();
		myUrlToParam = new HashMap<>();
	}

	/**
	 * Copy constructor
	 */
	private ReadOnlySearchParamCache(RuntimeSearchParamCache theRuntimeSearchParamCache) {
		myResourceNameToSpNameToSp = theRuntimeSearchParamCache.myResourceNameToSpNameToSp;
		myUrlToParam = theRuntimeSearchParamCache.myUrlToParam;
	}

	public Stream<RuntimeSearchParam> getSearchParamStream() {
		return myResourceNameToSpNameToSp.values().stream().flatMap(entry -> entry.values().stream());
	}

	protected Map<String, RuntimeSearchParam> getSearchParamMap(String theResourceName) {
		Map<String, RuntimeSearchParam> retval = myResourceNameToSpNameToSp.get(theResourceName);
		if (retval == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(myResourceNameToSpNameToSp.get(theResourceName));
	}

	public int size() {
		return myResourceNameToSpNameToSp.size();
	}

	public RuntimeSearchParam getByUrl(String theUrl) {
		return myUrlToParam.get(theUrl);
	}

	public static ReadOnlySearchParamCache fromFhirContext(FhirContext theFhirContext, SearchParameterCanonicalizer theCanonicalizer) {
		assert theCanonicalizer != null;

		ReadOnlySearchParamCache retVal = new ReadOnlySearchParamCache();

		Set<String> resourceNames = theFhirContext.getResourceTypes();

		if (theFhirContext.getVersion().getVersion() == FhirVersionEnum.R4) {
			IBaseBundle allSearchParameterBundle = (IBaseBundle) theFhirContext.newJsonParser().parseResource(ClasspathUtil.loadResourceAsStream("org/hl7/fhir/r4/model/sp/search-parameters.json"));
			for (IBaseResource next : BundleUtil.toListOfResources(theFhirContext, allSearchParameterBundle)) {
				RuntimeSearchParam nextCanonical = theCanonicalizer.canonicalizeSearchParameter(next);
				if (nextCanonical != null) {
					Collection<String> base = nextCanonical.getBase();
					if (base.contains("Resource") || base.contains("DomainResource")) {
						base = resourceNames;
					}

					for (String nextBase : base) {
						Map<String, RuntimeSearchParam> nameToParam = retVal.myResourceNameToSpNameToSp.computeIfAbsent(nextBase, t -> new HashMap<>());
						String nextName = nextCanonical.getName();
						nameToParam.putIfAbsent(nextName, nextCanonical);
					}
				}
			}
		}

		for (String resourceName : resourceNames) {
			RuntimeResourceDefinition nextResDef = theFhirContext.getResourceDefinition(resourceName);
			String nextResourceName = nextResDef.getName();

			Map<String, RuntimeSearchParam> nameToParam = retVal.myResourceNameToSpNameToSp.computeIfAbsent(nextResourceName, t-> new HashMap<>());
			for (RuntimeSearchParam nextSp : nextResDef.getSearchParams()) {
				nameToParam.putIfAbsent(nextSp.getName(), nextSp);
			}
		}
		return retVal;
	}

	public static ReadOnlySearchParamCache fromRuntimeSearchParamCache(RuntimeSearchParamCache theRuntimeSearchParamCache) {
		return new ReadOnlySearchParamCache(theRuntimeSearchParamCache);
	}
}
