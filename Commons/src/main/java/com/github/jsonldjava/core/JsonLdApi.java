package com.github.jsonldjava.core;

import static com.github.jsonldjava.core.JsonLdConsts.RDF_FIRST;
import static com.github.jsonldjava.core.JsonLdConsts.RDF_LIST;
import static com.github.jsonldjava.core.JsonLdConsts.RDF_NIL;
import static com.github.jsonldjava.core.JsonLdConsts.RDF_REST;
import static com.github.jsonldjava.core.JsonLdConsts.RDF_TYPE;
import static com.github.jsonldjava.core.JsonLdUtils.isKeyword;
import static com.github.jsonldjava.utils.Obj.newMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.github.jsonldjava.core.JsonLdConsts.Embed;
import com.github.jsonldjava.core.JsonLdError.Error;
import com.github.jsonldjava.utils.Obj;
import com.google.common.collect.Lists;

import eu.neclab.ngsildbroker.commons.constants.AppConstants;
import eu.neclab.ngsildbroker.commons.constants.NGSIConstants;
import eu.neclab.ngsildbroker.commons.datatypes.terms.LanguageQueryTerm;
import eu.neclab.ngsildbroker.commons.enums.ErrorType;
import eu.neclab.ngsildbroker.commons.exceptions.ResponseException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * A container object to maintain state relating to JsonLdOptions and the
 * current Context, and push these into the relevant algorithms in
 * JsonLdProcessor as necessary.
 *
 * @author tristan
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class JsonLdApi {

	JsonLdOptions opts;
	Object value = null;
	Context context = null;

	/**
	 * Constructs an empty JsonLdApi object using the default JsonLdOptions, and
	 * without initialization.
	 */
	public JsonLdApi() {
		this(new JsonLdOptions(""));
	}

	/**
	 * Constructs a JsonLdApi object using the given object as the initial JSON-LD
	 * object, and the given JsonLdOptions.
	 *
	 * @param input The initial JSON-LD object.
	 * @param opts  The JsonLdOptions to use.
	 * @throws JsonLdError If there is an error initializing using the object and
	 *                     options.
	 */
	public JsonLdApi(Object input, JsonLdOptions opts) throws JsonLdError {
		this(opts);
		initialize(input, null);
	}

	/**
	 * Constructs a JsonLdApi object using the given object as the initial JSON-LD
	 * object, the given context, and the given JsonLdOptions.
	 *
	 * @param input   The initial JSON-LD object.
	 * @param context The initial context.
	 * @param opts    The JsonLdOptions to use.
	 * @throws JsonLdError If there is an error initializing using the object and
	 *                     options.
	 */
//	public JsonLdApi(Object input, Object context, JsonLdOptions opts) throws JsonLdError {
//		this(opts);
//		initialize(input, null);
//	}

	/**
	 * Constructs an empty JsonLdApi object using the given JsonLdOptions, and
	 * without initialization. <br>
	 * If the JsonLdOptions parameter is null, then the default options are used.
	 *
	 * @param opts The JsonLdOptions to use.
	 */
	public JsonLdApi(JsonLdOptions opts) {
		if (opts == null) {
			opts = new JsonLdOptions("");
		} else {
			this.opts = opts;
		}
	}

	/**
	 * Initializes this object by cloning the input object using
	 * {@link JsonLdUtils#clone(Object)}, and by parsing the context using
	 * {@link Context#parse(Object)}.
	 *
	 * @param input   The initial object, which is to be cloned and used in
	 *                operations.
	 * @param context The context object, which is to be parsed and used in
	 *                operations.
	 * @throws JsonLdError If there was an error cloning the object, or in parsing
	 *                     the context.
	 */
	private void initialize(Object input, Object context) throws JsonLdError {
		if (input instanceof List || input instanceof Map) {
			this.value = JsonLdUtils.clone(input);
		}
		// TODO: string/IO input
		this.context = new Context(opts);
//		if (context != null) {
//			this.context = this.context.parse(context, false);
//		}
	}
//	public Object compact(Context activeCtx, String activeProperty, Object element, boolean compactArrays, int endPoint)
//			throws JsonLdError {
//		return compact(activeCtx, activeProperty, element, compactArrays, endPoint, null, null);
//	}

	/***
	 * ____ _ _ _ _ _ _ / ___|___ _ __ ___ _ __ __ _ ___| |_ / \ | | __ _ ___ _
	 * __(_) |_| |__ _ __ ___ | | / _ \| '_ ` _ \| '_ \ / _` |/ __| __| / _ \ | |/
	 * _` |/ _ \| '__| | __| '_ \| '_ ` _ \ | |__| (_) | | | | | | |_) | (_| | (__|
	 * |_ / ___ \| | (_| | (_) | | | | |_| | | | | | | | | \____\___/|_| |_| |_|
	 * .__/ \__,_|\___|\__| /_/ \_\_|\__, |\___/|_| |_|\__|_| |_|_| |_| |_| |_|
	 * |___/
	 */

	/**
	 * Compaction Algorithm
	 *
	 * http://json-ld.org/spec/latest/json-ld-api/#compaction-algorithm
	 *
	 * @param activeCtx      The Active Context
	 * @param activeProperty The Active Property
	 * @param element        The current element
	 * @param compactArrays  True to compact arrays.
	 * @param langQuery
	 * @param options
	 * @return The compacted JSON-LD object.
	 * @throws JsonLdError If there was an error during compaction.
	 */
	public Object compact(Context activeCtx, String activeProperty, Object element, boolean compactArrays, int endPoint,
			Set<String> options, LanguageQueryTerm langQuery) throws JsonLdError {
		// 2)
		if (element instanceof List) {
			// 2.1)
			final List<Object> result = new ArrayList<Object>();
			// 2.2)
			for (final Object item : (List<Object>) element) {
				// 2.2.1)
				final Object compactedItem = compact(activeCtx, activeProperty, item, compactArrays, endPoint, options,
						langQuery);
				// 2.2.2)
				if (compactedItem != null) {
					final boolean isList = (compactedItem instanceof Map
							&& ((Map<String, Object>) compactedItem).containsKey(JsonLdConsts.LIST));

					if (isList) {
						result.add(((Map<String, Object>) compactedItem).get(JsonLdConsts.LIST));
					} else {
						result.add(compactedItem);
					}

				}
			}
			if (AppConstants.FORCE_ARRAY_FIELDS.contains(activeProperty)) {
				return result;
			}
			// 2.3)
			if (compactArrays && result.size() == 1 && activeCtx.getContainer(activeProperty) == null) {
				return result.get(0);
			}
			// 2.4)
			return result;
		}

		// 3)
		if (element instanceof Map) {
			// access helper
			final Map<String, Object> elem = (Map<String, Object>) element;
			boolean removeSysAttrs = options == null
					|| !options.contains(NGSIConstants.QUERY_PARAMETER_OPTIONS_SYSATTRS);
			boolean keyValue = options != null && (options.contains(NGSIConstants.QUERY_PARAMETER_OPTIONS_KEYVALUES)
					|| options.contains(NGSIConstants.QUERY_PARAMETER_OPTIONS_SIMPLIFIED));
			boolean concise = options != null && (options.contains(NGSIConstants.QUERY_PARAMETER_OPTIONS_KEYVALUES)
					|| options.contains(NGSIConstants.QUERY_PARAMETER_OPTIONS_SIMPLIFIED));
			// 4
			if (elem.containsKey(JsonLdConsts.VALUE) || elem.containsKey(JsonLdConsts.ID)) {
				final Object compactedValue = activeCtx.compactValue(activeProperty, elem);
				boolean isScalar = !(compactedValue instanceof Map || compactedValue instanceof List);
				// jsonld 1.1: 7 in https://w3c.github.io/json-ld-api/#algorithm-6
				boolean isJson = activeCtx.getTermDefinition(activeProperty) != null
						&& JsonLdConsts.JSON.equals(activeCtx.getTermDefinition(activeProperty).get(JsonLdConsts.TYPE));
				if (isScalar || isJson) {
					return compactedValue;
				}
			}
			// 5)
			final boolean insideReverse = (JsonLdConsts.REVERSE.equals(activeProperty));

			// 6)
			final Map<String, Object> result = newMap();
			// 7)

			final List<Object> listResult = new ArrayList<Object>();

			final List<String> keys = new ArrayList<String>(elem.keySet());
			Collections.sort(keys);
			boolean isGeoProperty = false;
			boolean isProperty = false;
			boolean isRelationship = false;
			boolean isListRelationship=false;
			boolean isLanguageProperty = false;
			boolean isVocabProperty = false;
			boolean isListProperty = false;
			boolean isLocalOnly = false;
			boolean isJsonProperty = false;
			for (String expandedProperty : keys) {

				Object expandedValue = elem.get(expandedProperty);
				// 7.1)
				if (removeSysAttrs && (NGSIConstants.NGSI_LD_CREATED_AT.equals(expandedProperty)
						|| NGSIConstants.NGSI_LD_MODIFIED_AT.equals(expandedProperty))) {
					continue;
				}
				if (JsonLdConsts.ID.equals(expandedProperty) || JsonLdConsts.TYPE.equals(expandedProperty)) {
					// TODO: Relabel these step numbers when spec changes
					// 7.1.3)
					final String alias = activeCtx.compactIri(expandedProperty, true);
					Object compactedValue;

					// 7.1.1)
					if (expandedValue instanceof String) {
						compactedValue = activeCtx.compactIri((String) expandedValue,
								JsonLdConsts.TYPE.equals(expandedProperty));
					}
					// 7.1.2)
					else {
						final List<String> types = new ArrayList<String>();
						// 7.1.2.2)
						for (final String expandedType : (List<String>) expandedValue) {
							types.add(activeCtx.compactIri(expandedType, true));
						}
						// 7.1.2.3)
						if (types.size() == 1//
								// see w3c/json-ld-syntax#74
								&& (!opts.getAllowContainerSetOnType() || !(activeCtx.getContainer(alias) != null
										&& activeCtx.getContainer(alias).equals(JsonLdConsts.SET)))) {
							compactedValue = types.get(0);
						} else {
							compactedValue = types;
						}
					}
					if ((keyValue || concise || langQuery != null) && JsonLdConsts.TYPE.equals(expandedProperty)) {
						switch (((List<String>) expandedValue).get(0)) {
							case NGSIConstants.NGSI_LD_PROPERTY:
								isProperty = true;

							case NGSIConstants.NGSI_LD_GEOPROPERTY:
								isGeoProperty = true;
								break;
							case NGSIConstants.NGSI_LD_LANGPROPERTY:
								isLanguageProperty = true;
								break;
							case NGSIConstants.NGSI_LD_JSON_PROPERTY:
								isJsonProperty = true;
								break;
							case NGSIConstants.NGSI_LD_RELATIONSHIP:
								isRelationship = true;
								break;
							case NGSIConstants.NGSI_LD_LISTRELATIONSHIP:
								isListRelationship = true;
								break;
							case NGSIConstants.NGSI_LD_VocabProperty:
								isVocabProperty = true;
								break;
							case NGSIConstants.NGSI_LD_ListProperty:
								isListProperty = true;
								break;
							case NGSIConstants.NGSI_LD_LOCALONLY:
								isLocalOnly = true;
								break;
						}
						if (!alias.equals(NGSIConstants.TYPE) && (keyValue || concise) ) {
							continue;
						}

					}
					// 7.1.4)
					result.put(alias, compactedValue);
					continue;
					// TODO: old add value code, see if it's still relevant?
					// addValue(rval, alias, compactedValue,
					// isArray(compactedValue)
					// && ((List<Object>) expandedValue).size() == 0);
				}
				if (langQuery != null && isLanguageProperty
						&& expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_LANGUAGE_MAP)) {
					// do language stuff
					result.put("type", "Property");
					boolean found = false;
					Object defaultLang = null;
					List<Map<String, Object>> tmp = (List<Map<String, Object>>) expandedValue;
					for (Tuple2<Set<String>, Float> tuple : langQuery.getEntries()) {
//						[
//				          {
//				            "@value": "Grand Place",
//				            "@language": "fr"
//				          },
//				          {
//				            "@value": "Grote Markt",
//				            "@language": "nl"
//				          }
//				        ]
						Object atLang = null;
						for (String lang : tuple.getItem1()) {
							for (Map<String, Object> entry : tmp) {
								atLang = entry.get(JsonLdConsts.LANGUAGE);
								if (atLang == null) {
									defaultLang = atLang;
								}
								if ((lang.equals("*") && atLang == null) || lang.equals(atLang)) {
									expandedValue = List.of(Map.of(JsonLdConsts.VALUE, entry.get(JsonLdConsts.VALUE)));
									found = true;
									break;
								}
							}
							if (found) {
								if (atLang != null) {
									result.put("lang", atLang);
								}
								break;
							}
							if (lang.equals("*") && !found) {
								expandedValue = List.of(Map.of(JsonLdConsts.VALUE, tmp.get(0).get(JsonLdConsts.VALUE)));
								found = true;
								break;
							}
						}
						if (found) {
							break;
						}
					}
					if (!found) {
						expandedValue = defaultLang;
						if (expandedValue == null) {
							result.put("lang",tmp.get(0).get(JsonLdConsts.LANGUAGE));
							expandedValue = List.of(Map.of(JsonLdConsts.VALUE, tmp.get(0).get(JsonLdConsts.VALUE)));
						}
					}
					expandedProperty = NGSIConstants.NGSI_LD_HAS_VALUE;
					isProperty = true;
					isLanguageProperty = false;
				}
				if (keyValue) {
					if (isProperty || isGeoProperty) {
						if (expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_VALUE)) {
							return compact(activeCtx, activeProperty, expandedValue, compactArrays, endPoint, null,
									null);
						} else {
							continue;
						}
					}
					else if(expandedValue instanceof List<?> ls && ls.get(0) instanceof Map<?,?> map
							&& map.containsKey(NGSIConstants.JSON_LD_TYPE)
							&& (map.get(NGSIConstants.JSON_LD_TYPE).toString().contains(NGSIConstants.NGSI_LD_VocabProperty)
							|| map.get(NGSIConstants.JSON_LD_TYPE).toString().contains(NGSIConstants.NGSI_LD_LANGPROPERTY)
							|| map.get(NGSIConstants.JSON_LD_TYPE).toString().contains(NGSIConstants.NGSI_LD_JSON_PROPERTY))){
							map.remove(NGSIConstants.JSON_LD_TYPE);
					}
					else if(isListProperty && expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_LIST)){
						return compact(activeCtx, NGSIConstants.LIST, expandedValue, compactArrays, endPoint, null,
								null);
					}
					else if (isRelationship) {
						if (expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_OBJECT)) {
							List<String> ids = new ArrayList<>();
							if(expandedValue instanceof List<?> lsIdsMap){
								lsIdsMap.forEach(idMap->{
									if(idMap instanceof Map<?,?> map) {
										ids.add((String) map.get(JsonLdConsts.ID));
									}
								});
							}
							return compact(activeCtx, activeProperty, ids/*expandedValue*/, compactArrays, endPoint, null,
									null);
						} else {
							continue;
						}
					}
					else if (isListRelationship) {
//						expanded value for ListRelationship
//						"https://uri.etsi.org/ngsi-ld/hasObjectList": [
//						{
//							"@list": [
//							{
//								"@value": "urn:ngsi-ld:Person:Alice"
//							},
//							{
//								"@value": "urn:ngsi-ld:Person:Bob"
//							}
//         				 ]
//						}
//					]
						if (expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_OBJECT_LIST)) {
							List<String> ids = new ArrayList<>();
							if(expandedValue instanceof List<?> lsIdsMap){
								lsIdsMap.forEach(listMap->{
									if(listMap instanceof Map<?,?> map) {
										((List<Map<String,String>>)map.get(JsonLdConsts.LIST)).forEach(valueMap ->{
											ids.add(valueMap.get(JsonLdConsts.VALUE));
										});
									}
								});
							}
							return compact(activeCtx, activeProperty, ids/*expandedValue*/, compactArrays, endPoint, null,
									null);
						} else {
							continue;
						}
					}
				} else if (concise) {
					if (expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_VALUE)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_LANGUAGE_MAP)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_OBJECT)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_OBJECT_LIST)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_OBSERVED_AT)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_MODIFIED_AT)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_CREATED_AT)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_SCOPE)
							|| expandedProperty.equals(NGSIConstants.NGSI_LD_HAS_JSON)) {

					} else if (expandedValue instanceof List && ((List) expandedValue).get(0) instanceof Map
							&& ((List<Map>) expandedValue).get(0).containsKey(NGSIConstants.JSON_LD_TYPE)) {
						String tmp = ((List<String>) ((List<Map>) expandedValue).get(0).get(NGSIConstants.JSON_LD_TYPE))
								.get(0);

						switch (tmp) {
						case NGSIConstants.NGSI_LD_PROPERTY:
						case NGSIConstants.NGSI_LD_RELATIONSHIP:
						case NGSIConstants.NGSI_LD_GEOPROPERTY:
						case NGSIConstants.NGSI_LD_LANGPROPERTY:
							break;
						default:
							continue;
						}
					} else {
						continue;
					}
				}
				// 7.2)
				if (JsonLdConsts.REVERSE.equals(expandedProperty)) {
					// 7.2.1)
					final Map<String, Object> compactedValue = (Map<String, Object>) compact(activeCtx,
							JsonLdConsts.REVERSE, expandedValue, compactArrays, endPoint, options, langQuery);

					// 7.2.2)
					// Note: Must create a new set to avoid modifying the set we
					// are iterating over
					for (final String property : new HashSet<String>(compactedValue.keySet())) {
						final Object value = compactedValue.get(property);
						// 7.2.2.1)
						if (activeCtx.isReverseProperty(property)) {
							// 7.2.2.1.1)
							if ((JsonLdConsts.SET.equals(activeCtx.getContainer(property)) || !compactArrays)
									&& !(value instanceof List)) {
								final List<Object> tmp = new ArrayList<Object>();
								tmp.add(value);
								result.put(property, tmp);
							}
							// 7.2.2.1.2)
							if (!result.containsKey(property)) {
								result.put(property, value);
							}
							// 7.2.2.1.3)
							else {
								if (!(result.get(property) instanceof List)) {
									final List<Object> tmp = new ArrayList<Object>();
									tmp.add(result.put(property, tmp));
								}
								if (value instanceof List) {
									((List<Object>) result.get(property)).addAll((List<Object>) value);
								} else {
									((List<Object>) result.get(property)).add(value);
								}
							}
							// 7.2.2.1.4)
							compactedValue.remove(property);
						}
					}
					// 7.2.3)
					if (!compactedValue.isEmpty()) {
						// 7.2.3.1)
						final String alias = activeCtx.compactIri(JsonLdConsts.REVERSE, true);
						// 7.2.3.2)
						result.put(alias, compactedValue);
					}
					// 7.2.4)
					continue;
				}

				// 7.3)
				if (JsonLdConsts.INDEX.equals(expandedProperty)
						&& JsonLdConsts.INDEX.equals(activeCtx.getContainer(activeProperty))) {
					continue;
				}
				// 7.4)
				else if (JsonLdConsts.INDEX.equals(expandedProperty) || JsonLdConsts.VALUE.equals(expandedProperty)
						|| JsonLdConsts.LANGUAGE.equals(expandedProperty)) {
					// 7.4.1)
					final String alias = activeCtx.compactIri(expandedProperty, true);
					// 7.4.2)
					result.put(alias, expandedValue);
					continue;
				}

				// NOTE: expanded value must be an array due to expansion
				// algorithm.
				// NGSI NOTE: we just make it a list if it isn't one that can happen if we
				if (!(expandedValue instanceof List)) {
					ArrayList<Object> tmp = new ArrayList<Object>();
					tmp.add(expandedValue);
					expandedValue = tmp;
					// throw new JsonLdError(Error.NOT_IMPLEMENTED, "no array: " + expandedValue);
				}
				// 7.5)
				if (((List<Object>) expandedValue).isEmpty()) {
					// 7.5.1)
					final String itemActiveProperty = activeCtx.compactIri(expandedProperty, expandedValue, true,
							insideReverse);
					// 7.5.2)
					if (!result.containsKey(itemActiveProperty)) {
						result.put(itemActiveProperty, new ArrayList<Object>());
					} else {
						final Object value = result.get(itemActiveProperty);
						if (!(value instanceof List)) {
							final List<Object> tmp = new ArrayList<Object>();
							tmp.add(value);
							result.put(itemActiveProperty, tmp);
						}
					}
				}

				// 7.6)
				for (final Object expandedItem : (List<Object>) expandedValue) {

					// 7.6.1)
					final String itemActiveProperty = activeCtx.compactIri(expandedProperty, expandedItem, true,
							insideReverse);
					// 7.6.2)
					final String container = activeCtx.getContainer(itemActiveProperty);

					// get @list value if appropriate
					final boolean isList = (expandedItem instanceof Map
							&& ((Map<String, Object>) expandedItem).containsKey(JsonLdConsts.LIST));
					Object list = null;
					if (isList) {
						list = ((Map<String, Object>) expandedItem).get(JsonLdConsts.LIST);
					}

					// 7.6.3)
					Object compactedItem = compact(activeCtx, itemActiveProperty, isList ? list : expandedItem,
							compactArrays, endPoint, options, langQuery);

					// 7.6.4)
					if (isList) {
						// 7.6.4.1)
						if (!(compactedItem instanceof List)) {
							final List<Object> tmp = new ArrayList<Object>();
							tmp.add(compactedItem);
							compactedItem = tmp;

						} else {
						}
						// 7.6.4.2)
						if (!JsonLdConsts.LIST.equals(container)) {
							// 7.6.4.2.1)
							final Map<String, Object> wrapper = newMap();
							// TODO: SPEC: no mention of vocab = true
							wrapper.put(activeCtx.compactIri(JsonLdConsts.LIST, true), compactedItem);
							compactedItem = wrapper;

							// 7.6.4.2.2)
							if (((Map<String, Object>) expandedItem).containsKey(JsonLdConsts.INDEX)) {
								((Map<String, Object>) compactedItem).put(
										// TODO: SPEC: no mention of vocab =
										// true
										activeCtx.compactIri(JsonLdConsts.INDEX, true),
										((Map<String, Object>) expandedItem).get(JsonLdConsts.INDEX));
							}

						} else if (JsonLdConsts.LIST.equals(itemActiveProperty)) {
							listResult.add(compactedItem);
						}
						// 7.6.4.3)
						/*
						 * else if (result.containsKey(itemActiveProperty)) { throw new
						 * JsonLdError(Error.COMPACTION_TO_LIST_OF_LISTS,
						 * "There cannot be two list objects associated with an active property that has a container mapping"
						 * ); }
						 */
					}

					// 7.6.5)
					if (JsonLdConsts.LANGUAGE.equals(container) || JsonLdConsts.INDEX.equals(container)) {
						// 7.6.5.1)
						Map<String, Object> mapObject;
						if (result.containsKey(itemActiveProperty)) {
							mapObject = (Map<String, Object>) result.get(itemActiveProperty);
						} else {
							mapObject = newMap();
							result.put(itemActiveProperty, mapObject);
						}

						// 7.6.5.2)
						if (JsonLdConsts.LANGUAGE.equals(container) && (compactedItem instanceof Map
								&& ((Map<String, Object>) compactedItem).containsKey(JsonLdConsts.VALUE))) {
							compactedItem = ((Map<String, Object>) compactedItem).get(JsonLdConsts.VALUE);
						}

						// 7.6.5.3)
						final String mapKey = (String) ((Map<String, Object>) expandedItem).get(container);
						// 7.6.5.4)
						if (!mapObject.containsKey(mapKey)) {
							mapObject.put(mapKey, compactedItem);
						} else {
							List<Object> tmp;
							if (!(mapObject.get(mapKey) instanceof List)) {
								tmp = new ArrayList<Object>();
								tmp.add(mapObject.put(mapKey, tmp));
							} else {
								tmp = (List<Object>) mapObject.get(mapKey);
							}
							tmp.add(compactedItem);
						}
					}
					// 7.6.6)
					else {
						// 7.6.6.1)
						final Boolean check = (!compactArrays || JsonLdConsts.SET.equals(container)
								|| JsonLdConsts.GRAPH.equals(expandedProperty)) && (!(compactedItem instanceof List));

						if (isList && JsonLdConsts.LIST.equals(itemActiveProperty)) {
							continue;
						}
						if (check) {
							final List<Object> tmp = new ArrayList<Object>();
							tmp.add(compactedItem);
							compactedItem = tmp;
						}
						// 7.6.6.2)
						if (!result.containsKey(itemActiveProperty)
								&& !AppConstants.FORCE_ARRAY_FIELDS.contains(expandedProperty)) {
							result.put(itemActiveProperty, compactedItem);
						} else {
							if (!(result.get(itemActiveProperty) instanceof List)) {
								final List<Object> tmp = new ArrayList<Object>();
								if (result.containsKey(itemActiveProperty)) {
									tmp.add(result.get(itemActiveProperty));
								}
								result.put(itemActiveProperty, tmp);
							}
							if (compactedItem instanceof List) {
								((List<Object>) result.get(itemActiveProperty)).addAll((List<Object>) compactedItem);
							} else {
								((List<Object>) result.get(itemActiveProperty)).add(compactedItem);
							}
						}

					}
				}
			}
			// 8)
			if (concise && result.size() == 1 && result.containsKey("value")) {
				return result.get("value");
			}

			if (listResult.isEmpty()) {
				return result;
			} else {
				return listResult;
			}
		}

		// 2)
		return element;

	}

	public Object expandWithCoreContext(Object geoProp) {
		try {
			return expandSubLevels(JsonLdProcessor.getCoreContextClone(), NGSIConstants.NGSI_LD_LOCATION,
					new NGSIObject(geoProp, null), -2, false).getElement();
		} catch (JsonLdError | ResponseException e) {
			// should never happen
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Compaction Algorithm
	 *
	 * http://json-ld.org/spec/latest/json-ld-api/#compaction-algorithm
	 *
	 * @param activeCtx      The Active Context
	 * @param activeProperty The Active Property
	 * @param element        The current element
	 * @return The compacted JSON-LD object.
	 * @throws JsonLdError If there was an error during compaction.
	 */
	public Object compact(Context activeCtx, String activeProperty, Object element) throws JsonLdError {
		return compact(activeCtx, activeProperty, element, JsonLdOptions.DEFAULT_COMPACT_ARRAYS, -1, null, null);
	}

	/***
	 * _____ _ _ _ _ _ _ | ____|_ ___ __ __ _ _ __ __| | / \ | | __ _ ___ _ __(_)
	 * |_| |__ _ __ ___ | _| \ \/ / '_ \ / _` | '_ \ / _` | / _ \ | |/ _` |/ _ \|
	 * '__| | __| '_ \| '_ ` _ \ | |___ > <| |_) | (_| | | | | (_| | / ___ \| | (_|
	 * | (_) | | | | |_| | | | | | | | | |_____/_/\_\ .__/ \__,_|_| |_|\__,_| /_/
	 * \_\_|\__, |\___/|_| |_|\__|_| |_|_| |_| |_| |_| |___/
	 */

	/**
	 * Expansion Algorithm
	 *
	 * http://json-ld.org/spec/latest/json-ld-api/#expansion-algorithm
	 *
	 * @param activeCtx      The Active Context
	 * @param activeProperty The Active Property
	 * @param element        The current element
	 * @return The expanded JSON-LD object.
	 * @throws JsonLdError If there was an error during expansion.
	 */

	public Uni<NGSIObject> expand(Context activeCtx, String activeProperty, NGSIObject ngsiElement, int payloadType,
			boolean atContextAllowed, WebClient webClient,String atContextUrl) {
		final boolean frameExpansion = this.opts.getFrameExpansion();
		// 1)
		if (ngsiElement.getElement() == null) {
			return Uni.createFrom().item(new NGSIObject(null, null));
		}
		Object element = ngsiElement.getElement();
		// GK: This would be the point to set `propertyScopedContext` to the `@context`
		// entry for any term definition associated with `activeProperty`.
		// 3)
		if (element instanceof List) {
			// 3.1)
			ngsiElement.setArray(true);

			List<Uni<NGSIObject>> unis = Lists.newArrayList();
			for (final Object item : (List<Object>) element) {
				// 3.2.1)
				unis.add(expand(activeCtx, activeProperty,
						new NGSIObject(item, ngsiElement)
								.setFromHasValue(ngsiElement.isHasAtValue() || ngsiElement.isFromHasValue()),
						payloadType, atContextAllowed, webClient,atContextUrl));
			}

			return Uni.combine().all().unis(unis).combinedWith(list -> list).onItem().transformToUni(list -> {
				final List<Object> result = new ArrayList<Object>();
				// 3.2)
				NGSIObject resultElement = new NGSIObject(null, ngsiElement);
				resultElement.fillUpForArray(ngsiElement);
				resultElement.setFromHasValue(ngsiElement.isFromHasValue() || ngsiElement.isHasAtValue());
				for (Object obj : list) {
					NGSIObject ngsiV = (NGSIObject) obj;
					final Object v = ngsiV.getElement();
					resultElement.fillUpForArray(ngsiV);
					// 3.2.2)
					if ((JsonLdConsts.LIST.equals(activeProperty)
							|| JsonLdConsts.LIST.equals(activeCtx.getContainer(activeProperty)))
							&& (v instanceof List || (v instanceof Map
									&& ((Map<String, Object>) v).containsKey(JsonLdConsts.LIST)))) {
						// throw new JsonLdError(Error.LIST_OF_LISTS, "lists of lists are not
						// permitted.");
						if (v instanceof List) {
							Object expandedValue = newMap();
							((Map<String, Object>) expandedValue).put(JsonLdConsts.LIST, v);
							result.add(expandedValue);
						} else if (v instanceof Map) {
							result.add(v);
						}

					}
					// 3.2.3)
					else {

						if (v != null) {
							if (v instanceof List) {
								result.addAll((Collection<? extends Object>) v);
							} else {
								result.add(v);
							}
						}
					}

				}
				resultElement.setElement(result);
				try {
					resultElement.validate(payloadType, activeProperty,
							activeCtx.expandIri(activeProperty, false, true, null, null), this);
				} catch (JsonLdError | ResponseException e) {
					return Uni.createFrom().failure(e);
				}
				return Uni.createFrom().item(resultElement);
			});

		}
		// 4)
		else if (element instanceof Map) {
			// access helper
			final Map<String, Object> elem = (Map<String, Object>) element;
			// 5)
			// This would be the place to revert the active context from any previous
			// type-scoped context if the active context has a `previousContext` entry (with
			// some exceptions when called from a map, or if it's a value object or a
			// subject reference).
			// GK: If we found a `propertyScopedContext` above, we can parse it to create a
			// new activeCtx using the `override protected` option
			ngsiElement.setAtContextRequired(atContextAllowed);
			Uni<Context> ctxUni;
			Object bodyContext = elem.remove(JsonLdConsts.CONTEXT);
			if (bodyContext != null) {
				ngsiElement.setHasAtContext(true);
				if (!atContextAllowed) {
					return Uni.createFrom().failure(
							new ResponseException(ErrorType.BadRequestData, "@context entry in body is not allowed"));
				}
				ctxUni = activeCtx.parse(bodyContext, true, webClient,atContextUrl);
			} else {
				ctxUni = Uni.createFrom().item(activeCtx);
			}
			return ctxUni.onItem().transformToUni(ctx -> {
				try {
					return Uni.createFrom()
							.item(expandSubLevels(ctx, activeProperty, ngsiElement, payloadType, atContextAllowed));
				} catch (JsonLdError | ResponseException e) {
					return Uni.createFrom().failure(e);
				}
			});
		} else {
			// 2.1)
			if (activeProperty == null) {
				return Uni.createFrom()
						.failure(new ResponseException(ErrorType.BadRequestData, "null values are not allowed"));
			}
			if (JsonLdConsts.GRAPH.equals(activeProperty)) {
				return Uni.createFrom().item(new NGSIObject(null, ngsiElement));
			}
			String expandedProperty = activeCtx.expandIri(activeProperty, false, true, null, null);
			Object result = activeCtx.expandValue(activeProperty, element);
			ngsiElement.setElement(result);
			ngsiElement.setScalar(true);
			try {
				ngsiElement.validate(payloadType, activeProperty, expandedProperty, this);
			} catch (ResponseException e) {
				return Uni.createFrom().failure(e);
			}
			return Uni.createFrom().item(ngsiElement);
		}

	}

	public NGSIObject expandSubLevels(Context activeCtx, String activeProperty, NGSIObject ngsiElement, int payloadType,
			boolean atContextAllowed) throws JsonLdError, ResponseException {
		final boolean frameExpansion = this.opts.getFrameExpansion();
		// 1)
		if (ngsiElement.getElement() == null) {
			return new NGSIObject(null, null);
		}
		Object element = ngsiElement.getElement();
		// GK: This would be the point to set `propertyScopedContext` to the `@context`
		// entry for any term definition associated with `activeProperty`.
		// 3)
		if (element instanceof List) {
			// 3.1)
			ngsiElement.setArray(true);
			final List<Object> result = new ArrayList<Object>();
			// 3.2)
			NGSIObject resultElement = new NGSIObject(null, ngsiElement);
			resultElement.fillUpForArray(ngsiElement);
			resultElement.setFromHasValue(ngsiElement.isFromHasValue() || ngsiElement.isHasAtValue());
			for (final Object item : (List<Object>) element) {
				// 3.2.1)
				NGSIObject ngsiV = expandSubLevels(activeCtx, activeProperty,
						new NGSIObject(item, ngsiElement)
								.setFromHasValue(ngsiElement.isHasAtValue() || ngsiElement.isFromHasValue()),
						payloadType, atContextAllowed);
				final Object v = ngsiV.getElement();
				resultElement.fillUpForArray(ngsiV);
				// 3.2.2)
				if ((JsonLdConsts.LIST.equals(activeProperty)
						|| JsonLdConsts.LIST.equals(activeCtx.getContainer(activeProperty)))
						&& (v instanceof List
								|| (v instanceof Map && ((Map<String, Object>) v).containsKey(JsonLdConsts.LIST)))) {
					// throw new JsonLdError(Error.LIST_OF_LISTS, "lists of lists are not
					// permitted.");
					if (v instanceof List) {
						Object expandedValue = newMap();
						((Map<String, Object>) expandedValue).put(JsonLdConsts.LIST, v);
						result.add(expandedValue);
					} else if (v instanceof Map) {
						result.add(v);
					}

				}
				// 3.2.3)
				else {

					if (v != null) {
						if (v instanceof List) {
							result.addAll((Collection<? extends Object>) v);
						} else {
							result.add(v);
						}
					}
				}
			}
			resultElement.setElement(result);
			resultElement.validate(payloadType, activeProperty,
					activeCtx.expandIri(activeProperty, false, true, null, null), this);
			return resultElement;
		}
		// 4)
		else if (element instanceof Map)

		{
			// access helper
			final Map<String, Object> elem = (Map<String, Object>) element;
			// 5)
			// This would be the place to revert the active context from any previous
			// type-scoped context if the active context has a `previousContext` entry (with
			// some exceptions when called from a map, or if it's a value object or a
			// subject reference).
			// GK: If we found a `propertyScopedContext` above, we can parse it to create a
			// new activeCtx using the `override protected` option
//			ngsiElement.setAtContextRequired(atContextAllowed);
//			if (elem.containsKey(JsonLdConsts.CONTEXT)) {
//				ngsiElement.setHasAtContext(true);
//				if (!atContextAllowed) {
//					throw new ResponseException(ErrorType.BadRequestData, "@context entry in body is not allowed");
//				}
//				activeCtx = activeCtx.parse(elem.get(JsonLdConsts.CONTEXT), true);
//			}
			// GK: This would be the place to remember this version of activeCtx as
			// `typeScopedContext`.
			// 6)
			Map<String, Object> result = newMap();
			// 7)
			final List<String> keys = new ArrayList<String>(elem.keySet());
			Collections.sort(keys);
			// GK: This is the place to check for a type-scoped context by checking any key
			// that expands to `@type` to see the current context has a term that equals
			// that key where the term definition includes `@context`, updating the
			// activeCtx as you go (but using termScopedContext when checking the keys).
			// GK: 1.1 made the following loop somewhat recursive, due to nesting, so might
			// want to extract into a method.
			for (final String key : keys) {
				final Object value = elem.get(key);
				// 7.1)
				if (key.equals(JsonLdConsts.CONTEXT)) {
					continue;
				}
				// 7.2)
				final String expandedProperty = activeCtx.expandIri(key, false, true, null, null);
				ngsiElement.setExpandedProperty(expandedProperty);
				Object expandedValue = null;
				// 7.3)
				if (expandedProperty == null || (!expandedProperty.contains(":") && !isKeyword(expandedProperty))) {
					continue;
				}
				// 7.4)
				if (isKeyword(expandedProperty)) {
					// 7.4.1)
					ngsiElement.setLdKeyWord(true);
					if (JsonLdConsts.REVERSE.equals(activeProperty)) {
						throw new JsonLdError(Error.INVALID_REVERSE_PROPERTY_MAP,
								"a keyword cannot be used as a @reverse propery");
					}
					// 7.4.2)
					if (result.containsKey(expandedProperty)) {
						throw new JsonLdError(Error.COLLIDING_KEYWORDS, expandedProperty + " already exists in result");
					}
					// jsonld 1.1: 12 in https://w3c.github.io/json-ld-api/#algorithm-3
					Object inputType = elem.get(JsonLdConsts.TYPE);
					// 7.4.3)
					if (JsonLdConsts.ID.equals(expandedProperty)) {
						ngsiElement.setHasAtId(true);
						if (value instanceof String) {
							// TODO discuss this with martin afaik in ngsild ids need to be already uris in
							// the
							// compacted version and should not be expanded
							// expandedValue = activeCtx.expandIri((String) value, true, false, null, null);

							if (!((String) value).contains(":") && !ngsiElement.isFromHasValue()) {
								throw new ResponseException(ErrorType.BadRequestData, "IDs need to be URIs");
							}
							expandedValue = activeCtx.expandIri((String) value, true, false, null, null);

							// NGSICOMMENT: LD at the moment has no scenario where ids are arrays if this
							// changes this needs to be updated
							ngsiElement.setId((String) expandedValue);
						} else if (frameExpansion) {
							if (value instanceof Map) {
								if (((Map<String, Object>) value).size() != 0) {
									throw new JsonLdError(Error.INVALID_ID_VALUE,
											"@id value must be a an empty object for framing");
								}
								expandedValue = value;
							} else if (value instanceof List) {
								expandedValue = new ArrayList<String>();
								for (final Object v : (List<Object>) value) {
									if (!(v instanceof String)) {
										throw new JsonLdError(Error.INVALID_ID_VALUE,
												"@id value must be a string, an array of strings or an empty dictionary");
									}
									((List<String>) expandedValue)
											.add(activeCtx.expandIri((String) v, true, true, null, null));
								}
							} else {
								throw new JsonLdError(Error.INVALID_ID_VALUE,
										"value of @id must be a string, an array of strings or an empty dictionary");
							}
						} else {
							throw new JsonLdError(Error.INVALID_ID_VALUE, "value of @id must be a string");
						}
					}
					// 7.4.4)
					else if (JsonLdConsts.TYPE.equals(expandedProperty)) {
						ngsiElement.setHasAtType(true);
						if (value instanceof List) {
							expandedValue = new ArrayList<String>();
							for (final Object v : (List) value) {
//								if (!ngsiElement.isFromHasValue()) {
									if (!(v instanceof String)) {
										throw new JsonLdError(Error.INVALID_TYPE_VALUE,
												"@type value must be a string or array of strings");
									}
									String type = activeCtx.expandIri((String) v, true, true, null, null);
									((List<String>) expandedValue).add(type);
									ngsiElement.addType(type);
//								}
							}
						} else if (value instanceof String) {
							expandedValue = activeCtx.expandIri((String) value, true, true, null, null);
							ngsiElement.addType((String) expandedValue);
						}
						// TODO: SPEC: no mention of empty map check
						else if (frameExpansion && value instanceof Map) {
							if (!((Map<String, Object>) value).isEmpty()) {
								throw new JsonLdError(Error.INVALID_TYPE_VALUE,
										"@type value must be a an empty object for framing");
							}
							expandedValue = value;
							ngsiElement.addType((String) expandedValue);
						} else {
							throw new JsonLdError(Error.INVALID_TYPE_VALUE,
									"@type value must be a string or array of strings");
						}
					}
					// 7.4.5)
					else if (JsonLdConsts.GRAPH.equals(expandedProperty)) {
						NGSIObject ngsiExpandedValue = expandSubLevels(activeCtx, JsonLdConsts.GRAPH,
								new NGSIObject(value, ngsiElement), payloadType, atContextAllowed);
						expandedValue = ngsiExpandedValue.getElement();
					}
					// 7.4.6)
					else if (JsonLdConsts.VALUE.equals(expandedProperty)) {
						// jsonld 1.1: 13.4.7.1 in https://w3c.github.io/json-ld-api/#algorithm-3
						if (JsonLdConsts.JSON.equals(inputType)) {
							expandedValue = value;
							if (opts.getProcessingMode().equals(JsonLdOptions.JSON_LD_1_0)) {
								throw new JsonLdError(Error.INVALID_VALUE_OBJECT_VALUE, value);
							}
						}
						// jsonld 1.1: 13.4.7.2 in https://w3c.github.io/json-ld-api/#algorithm-3
						else if (value != null && (value instanceof Map || value instanceof List)) {
							throw new JsonLdError(Error.INVALID_VALUE_OBJECT_VALUE,
									"value of " + expandedProperty + " must be a scalar or null, but was: " + value);
						}
						// jsonld 1.1: 13.4.7.3 in https://w3c.github.io/json-ld-api/#algorithm-3
						else {
							expandedValue = value;
						}
						// jsonld 1.1: 13.4.7.4 in https://w3c.github.io/json-ld-api/#algorithm-3
						if (expandedValue == null) {
							result.put(JsonLdConsts.VALUE, null);
							continue;
						}
					}
					// 7.4.7)
					else if (JsonLdConsts.LANGUAGE.equals(expandedProperty)) {
						if (!(value instanceof String)) {
							throw new JsonLdError(Error.INVALID_LANGUAGE_TAGGED_STRING,
									"Value of " + expandedProperty + " must be a string");
						}
						expandedValue = ((String) value).toLowerCase();
					}
					// 7.4.8)
					else if (JsonLdConsts.INDEX.equals(expandedProperty)) {
						if (!(value instanceof String)) {
							throw new JsonLdError(Error.INVALID_INDEX_VALUE,
									"Value of " + expandedProperty + " must be a string");
						}
						expandedValue = value;
					}
					// 7.4.9)
					else if (JsonLdConsts.LIST.equals(expandedProperty)) {
						// 7.4.9.1)
						if (activeProperty == null || JsonLdConsts.GRAPH.equals(activeProperty)) {
							continue;
						}
						// 7.4.9.2)
						NGSIObject ngsiExpandedValue = expandSubLevels(activeCtx, activeProperty,
								new NGSIObject(value, ngsiElement), payloadType, atContextAllowed);
						expandedValue = ngsiExpandedValue.getElement();

						// NOTE: step not in the spec yet
						if (!(expandedValue instanceof List)) {
							final List<Object> tmp = new ArrayList<Object>();
							tmp.add(expandedValue);
							expandedValue = tmp;
						}

						// 7.4.9.3)
						/*
						 * for (final Object o : (List<Object>) expandedValue) { if (o instanceof Map &&
						 * ((Map<String, Object>) o).containsKey(JsonLdConsts.LIST)) { throw new
						 * JsonLdError(Error.LIST_OF_LISTS, "A list may not contain another list"); } }
						 */
					}
					// 7.4.10)
					else if (JsonLdConsts.SET.equals(expandedProperty)) {
						NGSIObject ngsiExpandedValue = expandSubLevels(activeCtx, activeProperty,
								new NGSIObject(value, ngsiElement), payloadType, atContextAllowed);
						expandedValue = ngsiExpandedValue.getElement();
					}
					// 7.4.11)
					else if (JsonLdConsts.REVERSE.equals(expandedProperty)) {
						if (!(value instanceof Map)) {
							throw new JsonLdError(Error.INVALID_REVERSE_VALUE, "@reverse value must be an object");
						}
						// 7.4.11.1)
						NGSIObject ngsiExpandedValue = expandSubLevels(activeCtx, JsonLdConsts.REVERSE,
								new NGSIObject(value, ngsiElement), payloadType, atContextAllowed);
						expandedValue = ngsiExpandedValue.getElement();
						// NOTE: algorithm assumes the result is a map
						// 7.4.11.2)
						if (((Map<String, Object>) expandedValue).containsKey(JsonLdConsts.REVERSE)) {
							final Map<String, Object> reverse = (Map<String, Object>) ((Map<String, Object>) expandedValue)
									.get(JsonLdConsts.REVERSE);
							for (final String property : reverse.keySet()) {
								final Object item = reverse.get(property);
								// 7.4.11.2.1)
								if (!result.containsKey(property)) {
									result.put(property, new ArrayList<Object>());
								}
								// 7.4.11.2.2)
								if (item instanceof List) {
									((List<Object>) result.get(property)).addAll((List<Object>) item);
								} else {
									((List<Object>) result.get(property)).add(item);
								}
							}
						}
						// 7.4.11.3)
						if (((Map<String, Object>) expandedValue)
								.size() > (((Map<String, Object>) expandedValue).containsKey(JsonLdConsts.REVERSE) ? 1
										: 0)) {
							// 7.4.11.3.1)
							if (!result.containsKey(JsonLdConsts.REVERSE)) {
								result.put(JsonLdConsts.REVERSE, newMap());
							}
							// 7.4.11.3.2)
							final Map<String, Object> reverseMap = (Map<String, Object>) result
									.get(JsonLdConsts.REVERSE);
							// 7.4.11.3.3)
							for (final String property : ((Map<String, Object>) expandedValue).keySet()) {
								if (JsonLdConsts.REVERSE.equals(property)) {
									continue;
								}
								// 7.4.11.3.3.1)
								final List<Object> items = (List<Object>) ((Map<String, Object>) expandedValue)
										.get(property);
								for (final Object item : items) {
									// 7.4.11.3.3.1.1)
									if (item instanceof Map
											&& (((Map<String, Object>) item).containsKey(JsonLdConsts.VALUE)
													|| ((Map<String, Object>) item).containsKey(JsonLdConsts.LIST))) {
										throw new JsonLdError(Error.INVALID_REVERSE_PROPERTY_VALUE);
									}
									// 7.4.11.3.3.1.2)
									if (!reverseMap.containsKey(property)) {
										reverseMap.put(property, new ArrayList<Object>());
									}
									// 7.4.11.3.3.1.3)
									((List<Object>) reverseMap.get(property)).add(item);
								}
							}
						}
						// GK: Also, `@included`, `@graph`, and `@direction`
						// 7.4.11.4)
						continue;
					}
					// TODO: SPEC no mention of @explicit etc in spec
					else if (frameExpansion && (JsonLdConsts.EXPLICIT.equals(expandedProperty)
							|| JsonLdConsts.DEFAULT.equals(expandedProperty)
							|| JsonLdConsts.EMBED.equals(expandedProperty)
							|| JsonLdConsts.REQUIRE_ALL.equals(expandedProperty)
							|| JsonLdConsts.EMBED_CHILDREN.equals(expandedProperty)
							|| JsonLdConsts.OMIT_DEFAULT.equals(expandedProperty))) {
						NGSIObject ngsiExpandedValue = expandSubLevels(activeCtx, expandedProperty,
								new NGSIObject(value, ngsiElement), payloadType, atContextAllowed);
						expandedValue = ngsiExpandedValue.getElement();
					}
					// 7.4.12)
					if (expandedValue != null) {
						/*
						 * jsonld 1.1: 13.4.16 in https://w3c.github.io/json-ld-api/#algorithm-3 if
						 * (!(expandedValue == null && JsonLdConsts.VALUE.equals(expandedProperty) &&
						 * (inputType == null || JsonLdConsts.JSON.equals(inputType)))) {
						 */
						result.put(expandedProperty, expandedValue);
					}
					// 7.4.13)
					continue;
				} else {
					switch (payloadType) {
					case AppConstants.ENTITY_CREATE_PAYLOAD:
					case AppConstants.MERGE_PATCH_PAYLOAD:
					case AppConstants.ENTITY_ATTRS_UPDATE_PAYLOAD:
					case AppConstants.ENTITY_UPDATE_PAYLOAD:
					case AppConstants.ENTITY_RETRIEVED_PAYLOAD:
					case AppConstants.TEMP_ENTITY_CREATE_PAYLOAD:
					case AppConstants.TEMP_ENTITY_UPDATE_PAYLOAD:
					case AppConstants.TEMP_ENTITY_RETRIEVED_PAYLOAD:
						if (NGSIConstants.NGSI_LD_HAS_VALUE.equals(expandedProperty)) {
							ngsiElement.setHasAtValue(true);
						} else if (NGSIConstants.NGSI_LD_HAS_VOCAB.equals(expandedProperty)) {
							ngsiElement.setHasVocab(true);
						}else if (NGSIConstants.NGSI_LD_HAS_JSON.equals(expandedProperty)) {
								ngsiElement.setHasJson(true);
						} else if (NGSIConstants.NGSI_LD_HAS_OBJECT.equals(expandedProperty)) {
							ngsiElement.setHasAtObject(true);
						}else if (NGSIConstants.NGSI_LD_HAS_OBJECT_LIST.equals(expandedProperty)) {
							ngsiElement.setHasListObject(true);
						}
						else if (NGSIConstants.NGSI_LD_DATE_TIME.equals(expandedProperty)) {
							ngsiElement.setDateTime(true);
						}
						break;
					case AppConstants.SUBSCRIPTION_CREATE_PAYLOAD:
					case AppConstants.SUBSCRIPTION_UPDATE_PAYLOAD:
						ngsiElement.resetSubscriptionVars();
						if (NGSIConstants.NGSI_LD_ENTITIES.equals(expandedProperty)) {
							ngsiElement.setEntities(true);
						} else if (NGSIConstants.NGSI_LD_GEO_QUERY.equals(expandedProperty)) {
							ngsiElement.setGeoQ(true);
						} else if (NGSIConstants.NGSI_LD_NOTIFICATION.equals(expandedProperty)) {
							ngsiElement.setNotificationEntry(true);
						} else if (NGSIConstants.NGSI_LD_TEMPORAL_QUERY.equals(expandedProperty)) {
							ngsiElement.setTemporalQ(true);
						} else if (NGSIConstants.NGSI_LD_ENDPOINT.equals(expandedProperty)) {
							ngsiElement.setEndpoint(true);
						} else if (NGSIConstants.NGSI_LD_NOTIFIERINFO.equals(expandedProperty)) {
							ngsiElement.setNotifierInfo(true);
						} else if (NGSIConstants.NGSI_LD_RECEIVERINFO.equals(expandedProperty)) {
							ngsiElement.setReceiverInfo(true);
						}
						break;
					default:
						break;
					}
				}

				// jsonld 1.1: 13.5 in https://w3c.github.io/json-ld-api/#algorithm-3
				String containerMapping = activeCtx.getContainer(key);
				// jsonld 1.1: 13.6 in https://w3c.github.io/json-ld-api/#algorithm-3
				if (activeCtx.getTermDefinition(key) != null
						&& JsonLdConsts.JSON.equals(activeCtx.getTermDefinition(key).get(JsonLdConsts.TYPE))) {
					Map<String, Object> newMap = newMap();
					newMap.put(JsonLdConsts.VALUE, value);
					newMap.put(JsonLdConsts.TYPE, JsonLdConsts.JSON);
					expandedValue = newMap;
				}
				// 7.5
				else if (JsonLdConsts.LANGUAGE.equals(containerMapping) && value instanceof Map) {
					// 7.5.1)
					expandedValue = new ArrayList<Object>();
					// 7.5.2)
					for (final String language : ((Map<String, Object>) value).keySet()) {
						Object languageValue = ((Map<String, Object>) value).get(language);
						// 7.5.2.1)
						if (!(languageValue instanceof List)) {
							final Object tmp = languageValue;
							languageValue = new ArrayList<Object>();
							((List<Object>) languageValue).add(tmp);
						}
						// 7.5.2.2)
						for (final Object item : (List<Object>) languageValue) {
							// jsonld 1.1: 13.7.4.2.1 in
							// https://w3c.github.io/json-ld-api/#expansion-algorithm
							if (item == null) {
								continue;
							}
							// 7.5.2.2.1)
							if (!(item instanceof String)) {
								throw new JsonLdError(Error.INVALID_LANGUAGE_MAP_VALUE,
										"Expected " + item.toString() + " to be a string");
							}
							// 7.5.2.2.2)
							final Map<String, Object> tmp = newMap();
							tmp.put(JsonLdConsts.VALUE, item);
							tmp.put(JsonLdConsts.LANGUAGE, language.toLowerCase());
							((List<Object>) expandedValue).add(tmp);
						}
					}
				}
				// 7.6)
				// GK: Also a place to see if key is `@json` for JSON literals.
				else if (JsonLdConsts.INDEX.equals(activeCtx.getContainer(key)) && value instanceof Map) {
					// 7.6.1)
					// GK: `@index` also supports property indexing, if the term definition includes
					// `@index`.
					// GK: A map can also include `@none`.
					expandedValue = new ArrayList<Object>();
					// 7.6.2)
					final List<String> indexKeys = new ArrayList<String>(((Map<String, Object>) value).keySet());
					Collections.sort(indexKeys);
					for (final String index : indexKeys) {
						Object indexValue = ((Map<String, Object>) value).get(index);
						// 7.6.2.1)
						if (!(indexValue instanceof List)) {
							final Object tmp = indexValue;
							indexValue = new ArrayList<Object>();
							((List<Object>) indexValue).add(tmp);
						}
						// 7.6.2.2)
						NGSIObject ngsiIndexValue = expandSubLevels(activeCtx, key,
								new NGSIObject(indexValue, ngsiElement), payloadType, atContextAllowed);
						indexValue = ngsiIndexValue.getElement();
						// 7.6.2.3)
						for (final Map<String, Object> item : (List<Map<String, Object>>) indexValue) {
							// 7.6.2.3.1)
							if (!item.containsKey(JsonLdConsts.INDEX)) {
								item.put(JsonLdConsts.INDEX, index);
							}
							// 7.6.2.3.2)
							((List<Object>) expandedValue).add(item);
						}
					}
				}
				// 7.7)
				else {

					NGSIObject ngsiExpandedValue = expandSubLevels(activeCtx, key,
							new NGSIObject(value, ngsiElement)
									.setFromHasValue(ngsiElement.isHasAtValue() || ngsiElement.isFromHasValue()),
							payloadType, atContextAllowed);
					ngsiElement.getDatasetIds().addAll(ngsiExpandedValue.getDatasetIds());
					expandedValue = ngsiExpandedValue.getElement();

				}
				// 7.8)
				if (expandedValue == null) {
					continue;
				}
				// 7.9)
				if (JsonLdConsts.LIST.equals(activeCtx.getContainer(key))) {
					if (!(expandedValue instanceof Map)
							|| !((Map<String, Object>) expandedValue).containsKey(JsonLdConsts.LIST)) {
						Object tmp = expandedValue;
						if (!(tmp instanceof List)) {
							tmp = new ArrayList<Object>();
							((List<Object>) tmp).add(expandedValue);
						}
						expandedValue = newMap();
						((Map<String, Object>) expandedValue).put(JsonLdConsts.LIST, tmp);
					}
				}
				// GK: Other container possibilities including `@graph`, `@id`, and `@type`
				// along with variations.
				// 7.10)
				if (activeCtx.isReverseProperty(key)) {
					// 7.10.1)
					if (!result.containsKey(JsonLdConsts.REVERSE)) {
						result.put(JsonLdConsts.REVERSE, newMap());
					}
					// 7.10.2)
					final Map<String, Object> reverseMap = (Map<String, Object>) result.get(JsonLdConsts.REVERSE);
					// 7.10.3)
					if (!(expandedValue instanceof List)) {
						final Object tmp = expandedValue;
						expandedValue = new ArrayList<Object>();
						((List<Object>) expandedValue).add(tmp);
					}
					// 7.10.4)
					for (final Object item : (List<Object>) expandedValue) {
						// 7.10.4.1)
						if (item instanceof Map && (((Map<String, Object>) item).containsKey(JsonLdConsts.VALUE)
								|| ((Map<String, Object>) item).containsKey(JsonLdConsts.LIST))) {
							throw new JsonLdError(Error.INVALID_REVERSE_PROPERTY_VALUE);
						}
						// 7.10.4.2)
						if (!reverseMap.containsKey(expandedProperty)) {
							reverseMap.put(expandedProperty, new ArrayList<Object>());
						}
						// 7.10.4.3)
						if (item instanceof List) {
							((List<Object>) reverseMap.get(expandedProperty)).addAll((List<Object>) item);
						} else {
							((List<Object>) reverseMap.get(expandedProperty)).add(item);
						}
					}
				}
				// 7.11)
				else {
					// 7.11.1)
					if (!result.containsKey(expandedProperty)) {
						result.put(expandedProperty, new ArrayList<Object>());
					}
					// 7.11.2)
					if (expandedValue instanceof List) {
						((List<Object>) result.get(expandedProperty)).addAll((List<Object>) expandedValue);
					} else {
						((List<Object>) result.get(expandedProperty)).add(expandedValue);
					}
				}
			}
			// 8)
			if (result.containsKey(JsonLdConsts.VALUE)) {
				// 8.1)
				// TODO: is this method faster than just using containsKey for
				// each?
				final Set<String> keySet = new HashSet<String>(result.keySet());
				keySet.remove(JsonLdConsts.VALUE);
				keySet.remove(JsonLdConsts.INDEX);
				final boolean langremoved = keySet.remove(JsonLdConsts.LANGUAGE);
				final boolean typeremoved = keySet.remove(JsonLdConsts.TYPE);
				if ((langremoved && typeremoved) || !keySet.isEmpty()) {
					throw new JsonLdError(Error.INVALID_VALUE_OBJECT, "value object has unknown keys");
				}
				// 8.2)
				final Object rval = result.get(JsonLdConsts.VALUE);
				if (rval == null) {
					// nothing else is possible with result if we set it to
					// null, so simply return it
					return new NGSIObject(null, ngsiElement);
				} else if (result.getOrDefault(JsonLdConsts.TYPE, "").equals(JsonLdConsts.JSON)) {
					// jsonld 1.1: 14.3 in https://w3c.github.io/json-ld-api/#algorithm-3
				}
				// 8.3)
				else if (!(rval instanceof String) && result.containsKey(JsonLdConsts.LANGUAGE)) {
					throw new JsonLdError(Error.INVALID_LANGUAGE_TAGGED_VALUE,
							"when @language is used, @value must be a string");
				}
				// 8.4)
				else if (result.containsKey(JsonLdConsts.TYPE)) {
					// TODO: is this enough for "is an IRI"
					if (!(result.get(JsonLdConsts.TYPE) instanceof String)
							|| ((String) result.get(JsonLdConsts.TYPE)).startsWith("_:")
							|| !((String) result.get(JsonLdConsts.TYPE)).contains(":")) {
						throw new JsonLdError(Error.INVALID_TYPED_VALUE, "value of @type must be an IRI");
					}
				}
			}
			// 9)
			else if (result.containsKey(JsonLdConsts.TYPE)) {
				final Object rtype = result.get(JsonLdConsts.TYPE);
				if (!(rtype instanceof List)) {
					final List<Object> tmp = new ArrayList<Object>();
					tmp.add(rtype);
					result.put(JsonLdConsts.TYPE, tmp);
				}
			}
			// 10)
			else if (result.containsKey(JsonLdConsts.SET) || result.containsKey(JsonLdConsts.LIST)) {
				// 10.1)
				if (result.size() > (result.containsKey(JsonLdConsts.INDEX) ? 2 : 1)) {
					throw new JsonLdError(Error.INVALID_SET_OR_LIST_OBJECT, "@set or @list may only contain @index");
				}
				// 10.2)
				if (result.containsKey(JsonLdConsts.SET)) {
					return new NGSIObject(result.get(JsonLdConsts.SET), ngsiElement);
				}
			}
			// 11)
			if (result.containsKey(JsonLdConsts.LANGUAGE) && result.size() == 1) {
				result = null;
			}
			// 12)
			if (activeProperty == null || JsonLdConsts.GRAPH.equals(activeProperty)) {
				// 12.1)
				if (result != null && (result.size() == 0 || result.containsKey(JsonLdConsts.VALUE)
						|| result.containsKey(JsonLdConsts.LIST))) {
					result = null;
				}
				// 12.2)
				else if (result != null && !frameExpansion && result.containsKey(JsonLdConsts.ID)
						&& result.size() == 1) {
					result = null;
				}
			}
			ngsiElement.setElement(result);
			ngsiElement.validate(payloadType, activeProperty,
					activeCtx.expandIri(activeProperty, false, true, null, null), this);
			return ngsiElement;
		}
		// 2) If element is a scalar
		else {
			// 2.1)
			if (activeProperty == null) {
				throw new ResponseException(ErrorType.BadRequestData, "null values are not allowed");
			}
			if (JsonLdConsts.GRAPH.equals(activeProperty)) {
				return new NGSIObject(null, ngsiElement);
			}
			String expandedProperty = activeCtx.expandIri(activeProperty, false, true, null, null);
			Object result = activeCtx.expandValue(activeProperty, element);
			ngsiElement.setElement(result);
			ngsiElement.setScalar(true);
			ngsiElement.validate(payloadType, activeProperty, expandedProperty, this);
			return ngsiElement;
		}
	}

	/**
	 * Expansion Algorithm
	 *
	 * http://json-ld.org/spec/latest/json-ld-api/#expansion-algorithm
	 *
	 * @param activeCtx The Active Context
	 * @param element   The current element
	 * @return The expanded JSON-LD object.
	 * @throws JsonLdError       If there was an error during expansion.
	 * @throws ResponseException
	 */
	public Uni<Object> expand(Context activeCtx, Object element, int payloadType, boolean atContextAllowed,
			WebClient webClient, String atContextUrl) {
		return expand(activeCtx, null, new NGSIObject(element, null), payloadType, atContextAllowed, webClient,atContextUrl).onItem()
				.transform(ngsiElem -> ngsiElem.getElement());
	}

	/***
	 * _____ _ _ _ _ _ _ _ _ | ___| | __ _| |_| |_ ___ _ __ / \ | | __ _ ___ _ __(_)
	 * |_| |__ _ __ ___ | |_ | |/ _` | __| __/ _ \ '_ \ / _ \ | |/ _` |/ _ \| '__| |
	 * __| '_ \| '_ ` _ \ | _| | | (_| | |_| || __/ | | | / ___ \| | (_| | (_) | | |
	 * | |_| | | | | | | | | |_| |_|\__,_|\__|\__\___|_| |_| /_/ \_\_|\__, |\___/|_|
	 * |_|\__|_| |_|_| |_| |_| |___/
	 */

	void generateNodeMap(Object element, Map<String, Object> nodeMap) throws JsonLdError {
		generateNodeMap(element, nodeMap, JsonLdConsts.DEFAULT, null, null, null);
	}

	void generateNodeMap(Object element, Map<String, Object> nodeMap, String activeGraph) throws JsonLdError {
		generateNodeMap(element, nodeMap, activeGraph, null, null, null);
	}

	void generateNodeMap(Object element, Map<String, Object> nodeMap, String activeGraph, Object activeSubject,
			String activeProperty, Map<String, Object> list) throws JsonLdError {
		// 1)
		if (element instanceof List) {
			// 1.1)
			for (final Object item : (List<Object>) element) {
				generateNodeMap(item, nodeMap, activeGraph, activeSubject, activeProperty, list);
			}
			return;
		}

		// for convenience
		final Map<String, Object> elem = (Map<String, Object>) element;

		// 2)
		if (!nodeMap.containsKey(activeGraph)) {
			nodeMap.put(activeGraph, newMap());
		}
		final Map<String, Object> graph = (Map<String, Object>) nodeMap.get(activeGraph);
		Map<String, Object> node = (Map<String, Object>) (activeSubject == null ? null : graph.get(activeSubject));

		// 3)
		if (elem.containsKey(JsonLdConsts.TYPE)) {
			// 3.1)
			List<String> oldTypes;
			final List<String> newTypes = new ArrayList<String>();
			if (elem.get(JsonLdConsts.TYPE) instanceof List) {
				oldTypes = (List<String>) elem.get(JsonLdConsts.TYPE);
			} else {
				oldTypes = new ArrayList<String>(4);
				oldTypes.add((String) elem.get(JsonLdConsts.TYPE));
			}
			for (final String item : oldTypes) {
				if (item.startsWith("_:")) {
					newTypes.add(generateBlankNodeIdentifier(item));
				} else {
					newTypes.add(item);
				}
			}
			if (elem.get(JsonLdConsts.TYPE) instanceof List) {
				elem.put(JsonLdConsts.TYPE, newTypes);
			} else {
				elem.put(JsonLdConsts.TYPE, newTypes.get(0));
			}
		}

		// 4)
		if (elem.containsKey(JsonLdConsts.VALUE)) {
			// 4.1)
			if (list == null) {
				JsonLdUtils.mergeValue(node, activeProperty, elem);
			}
			// 4.2)
			else {
				JsonLdUtils.mergeValue(list, JsonLdConsts.LIST, elem);
			}
		}

		// 5)
		else if (elem.containsKey(JsonLdConsts.LIST)) {
			// 5.1)
			final Map<String, Object> result = newMap(JsonLdConsts.LIST, new ArrayList<Object>(4));
			// 5.2)
			// for (final Object item : (List<Object>) elem.get("@list")) {
			// generateNodeMap(item, nodeMap, activeGraph, activeSubject,
			// activeProperty, result);
			// }
			generateNodeMap(elem.get(JsonLdConsts.LIST), nodeMap, activeGraph, activeSubject, activeProperty, result);
			// 5.3)
			JsonLdUtils.mergeValue(node, activeProperty, result);
		}

		// 6)
		else {
			// 6.1)
			String id = (String) elem.remove(JsonLdConsts.ID);
			if (id != null) {
				if (id.startsWith("_:")) {
					id = generateBlankNodeIdentifier(id);
				}
			}
			// 6.2)
			else {
				id = generateBlankNodeIdentifier(null);
			}
			// 6.3)
			if (!graph.containsKey(id)) {
				final Map<String, Object> tmp = newMap(JsonLdConsts.ID, id);
				graph.put(id, tmp);
			}
			// 6.4) TODO: SPEC this line is asked for by the spec, but it breaks
			// various tests
			// node = (Map<String, Object>) graph.get(id);
			// 6.5)
			if (activeSubject instanceof Map) {
				// 6.5.1)
				JsonLdUtils.mergeValue((Map<String, Object>) graph.get(id), activeProperty, activeSubject);
			}
			// 6.6)
			else if (activeProperty != null) {
				final Map<String, Object> reference = newMap(JsonLdConsts.ID, id);
				// 6.6.2)
				if (list == null) {
					// 6.6.2.1+2)
					JsonLdUtils.mergeValue(node, activeProperty, reference);
				}
				// 6.6.3) TODO: SPEC says to add ELEMENT to @list member, should
				// be REFERENCE
				else {
					JsonLdUtils.mergeValue(list, JsonLdConsts.LIST, reference);
				}
			}
			// TODO: SPEC this is removed in the spec now, but it's still needed
			// (see 6.4)
			node = (Map<String, Object>) graph.get(id);
			// 6.7)
			if (elem.containsKey(JsonLdConsts.TYPE)) {
				for (final Object type : (List<Object>) elem.remove(JsonLdConsts.TYPE)) {
					JsonLdUtils.mergeValue(node, JsonLdConsts.TYPE, type);
				}
			}
			// 6.8)
			if (elem.containsKey(JsonLdConsts.INDEX)) {
				final Object elemIndex = elem.remove(JsonLdConsts.INDEX);
				if (node.containsKey(JsonLdConsts.INDEX)) {
					if (!JsonLdUtils.deepCompare(node.get(JsonLdConsts.INDEX), elemIndex)) {
						throw new JsonLdError(Error.CONFLICTING_INDEXES);
					}
				} else {
					node.put(JsonLdConsts.INDEX, elemIndex);
				}
			}
			// 6.9)
			if (elem.containsKey(JsonLdConsts.REVERSE)) {
				// 6.9.1)
				final Map<String, Object> referencedNode = newMap(JsonLdConsts.ID, id);
				// 6.9.2+6.9.4)
				final Map<String, Object> reverseMap = (Map<String, Object>) elem.remove(JsonLdConsts.REVERSE);
				// 6.9.3)
				for (final String property : reverseMap.keySet()) {
					final List<Object> values = (List<Object>) reverseMap.get(property);
					// 6.9.3.1)
					for (final Object value : values) {
						// 6.9.3.1.1)
						generateNodeMap(value, nodeMap, activeGraph, referencedNode, property, null);
					}
				}
			}
			// 6.10)
			if (elem.containsKey(JsonLdConsts.GRAPH)) {
				generateNodeMap(elem.remove(JsonLdConsts.GRAPH), nodeMap, id, null, null, null);
			}
			// 6.11)
			final List<String> keys = new ArrayList<String>(elem.keySet());
			Collections.sort(keys);
			for (String property : keys) {
				final Object value = elem.get(property);
				// 6.11.1)
				if (property.startsWith("_:")) {
					property = generateBlankNodeIdentifier(property);
				}
				// 6.11.2)
				if (!node.containsKey(property)) {
					node.put(property, new ArrayList<Object>(4));
				}
				// 6.11.3)
				generateNodeMap(value, nodeMap, activeGraph, id, property, null);
			}
		}
	}

	/**
	 * Blank Node identifier map specified in:
	 *
	 * http://www.w3.org/TR/json-ld-api/#generate-blank-node-identifier
	 */
	private final Map<String, String> blankNodeIdentifierMap = new LinkedHashMap<String, String>();

	/**
	 * Counter specified in:
	 *
	 * http://www.w3.org/TR/json-ld-api/#generate-blank-node-identifier
	 */
	private int blankNodeCounter = 0;

	/**
	 * Generates a blank node identifier for the given key using the algorithm
	 * specified in:
	 *
	 * http://www.w3.org/TR/json-ld-api/#generate-blank-node-identifier
	 *
	 * @param id The id, or null to generate a fresh, unused, blank node identifier.
	 * @return A blank node identifier based on id if it was not null, or a fresh,
	 *         unused, blank node identifier if it was null.
	 */
	String generateBlankNodeIdentifier(String id) {
		if (id != null && blankNodeIdentifierMap.containsKey(id)) {
			return blankNodeIdentifierMap.get(id);
		}
		final String bnid = "_:b" + blankNodeCounter++;
		if (id != null) {
			blankNodeIdentifierMap.put(id, bnid);
		}
		return bnid;
	}

	/**
	 * Generates a fresh, unused, blank node identifier using the algorithm
	 * specified in:
	 *
	 * http://www.w3.org/TR/json-ld-api/#generate-blank-node-identifier
	 *
	 * @return A fresh, unused, blank node identifier.
	 */
	String generateBlankNodeIdentifier() {
		return generateBlankNodeIdentifier(null);
	}

	/***
	 * _____ _ _ _ _ _ _ | ___| __ __ _ _ __ ___ (_)_ __ __ _ / \ | | __ _ ___ _
	 * __(_) |_| |__ _ __ ___ | |_ | '__/ _` | '_ ` _ \| | '_ \ / _` | / _ \ | |/ _`
	 * |/ _ \| '__| | __| '_ \| '_ ` _ \ | _|| | | (_| | | | | | | | | | | (_| | /
	 * ___ \| | (_| | (_) | | | | |_| | | | | | | | | |_| |_| \__,_|_| |_| |_|_|_|
	 * |_|\__, | /_/ \_\_|\__, |\___/|_| |_|\__|_| |_|_| |_| |_| |___/ |___/
	 */

	private class FramingContext {
		public Embed embed;
		public boolean explicit;
		public boolean omitDefault;
		public Map<String, EmbedNode> uniqueEmbeds;
		public LinkedList<String> subjectStack;
		public boolean requireAll;

		public FramingContext() {
			embed = Embed.LAST;
			explicit = false;
			omitDefault = false;
			requireAll = false;
			uniqueEmbeds = new HashMap<>();
			subjectStack = new LinkedList<>();
		}

		public FramingContext(JsonLdOptions opts) {
			this();
			if (opts.getEmbed() != null) {
				this.embed = opts.getEmbedVal();
			}
			if (opts.getExplicit() != null) {
				this.explicit = opts.getExplicit();
			}
			if (opts.getOmitDefault() != null) {
				this.omitDefault = opts.getOmitDefault();
			}
			if (opts.getRequireAll() != null) {
				this.requireAll = opts.getRequireAll();
			}
		}
	}

	private class EmbedNode {
		public Object parent = null;
		public String property = null;

		public EmbedNode(Object parent, String property) {
			this.parent = parent;
			this.property = property;
		}
	}

	private Map<String, Object> nodeMap;

	/**
	 * Performs JSON-LD
	 * <a href="http://json-ld.org/spec/latest/json-ld-framing/">framing</a>.
	 *
	 * @param input the expanded JSON-LD to frame.
	 * @param frame the expanded JSON-LD frame to use.
	 * @return the framed output.
	 * @throws JsonLdError If the framing was not successful.
	 */
	public List<Object> frame(Object input, List<Object> frame) throws JsonLdError {
		// create framing state
		final FramingContext state = new FramingContext(this.opts);

		// use tree map so keys are sorted by default
		final Map<String, Object> nodes = new TreeMap<String, Object>();
		generateNodeMap(input, nodes);
		this.nodeMap = (Map<String, Object>) nodes.get(JsonLdConsts.DEFAULT);

		final List<Object> framed = new ArrayList<Object>();
		// NOTE: frame validation is done by the function not allowing anything
		// other than list to me passed
		// 1.
		// If frame is an array, set frame to the first member of the array,
		// which MUST be a valid frame.
		frame(state, this.nodeMap, (frame != null && frame.size() > 0 ? (Map<String, Object>) frame.get(0) : newMap()),
				framed, null);

		return framed;
	}

	private boolean createsCircularReference(String id, FramingContext state) {
		return state.subjectStack.contains(id);
	}

	/**
	 * Frames subjects according to the given frame.
	 *
	 * @param state    the current framing state.
	 * @param frame    the frame.
	 * @param parent   the parent subject or top-level array.
	 * @param property the parent property, initialized to null.
	 * @throws JsonLdError If there was an error during framing.
	 */
	private void frame(FramingContext state, Map<String, Object> nodes, Map<String, Object> frame, Object parent,
			String property) throws JsonLdError {

		// https://json-ld.org/spec/latest/json-ld-framing/#framing-algorithm

		// 2.
		// Initialize flags embed, explicit, and requireAll from object embed
		// flag,
		// explicit inclusion flag, and require all flag in state overriding
		// from
		// any property values for @embed, @explicit, and @requireAll in frame.
		// TODO: handle @requireAll
		final Embed embed = getFrameEmbed(frame, state.embed);
		final Boolean explicitOn = getFrameFlag(frame, JsonLdConsts.EXPLICIT, state.explicit);
		final Boolean requireAll = getFrameFlag(frame, JsonLdConsts.REQUIRE_ALL, state.requireAll);
		final Map<String, Object> flags = newMap();
		flags.put(JsonLdConsts.EXPLICIT, explicitOn);
		flags.put(JsonLdConsts.EMBED, embed);
		flags.put(JsonLdConsts.REQUIRE_ALL, requireAll);

		// 3.
		// Create a list of matched subjects by filtering subjects against frame
		// using the Frame Matching algorithm with state, subjects, frame, and
		// requireAll.
		final Map<String, Object> matches = filterNodes(state, nodes, frame, requireAll);
		final List<String> ids = new ArrayList<String>(matches.keySet());
		Collections.sort(ids);

		// 4.
		// Set link the the value of link in state associated with graph name in
		// state,
		// creating a new empty dictionary, if necessary.
		@SuppressWarnings("unused")
		final Map<String, EmbedNode> link = state.uniqueEmbeds;

		// 5.
		// For each id and associated node object node from the set of matched
		// subjects, ordered by id:
		for (final String id : ids) {
			@SuppressWarnings("unused")
			final Map<String, Object> subject = (Map<String, Object>) matches.get(id);

			// 5.1
			// Initialize output to a new dictionary with @id and id and add
			// output to link associated with id.
			final Map<String, Object> output = newMap();
			output.put(JsonLdConsts.ID, id);

			// 5.2
			// If embed is @link and id is in link, node already exists in
			// results.
			// Add the associated node object from link to parent and do not
			// perform
			// additional processing for this node.
			if (embed == Embed.LINK && state.uniqueEmbeds.containsKey(id)) {
				addFrameOutput(state, parent, property, state.uniqueEmbeds.get(id));
				continue;
			}

			// Occurs only at top level, compartmentalize each top-level match
			if (property == null) {
				state.uniqueEmbeds = new HashMap<>();
			}

			// 5.3
			// Otherwise, if embed is @never or if a circular reference would be
			// created by an embed,
			// add output to parent and do not perform additional processing for
			// this node.
			if (embed == Embed.NEVER || createsCircularReference(id, state)) {
				addFrameOutput(state, parent, property, output);
				continue;
			}

			// 5.4
			// Otherwise, if embed is @last, remove any existing embedded node
			// from parent associated
			// with graph name in state. Requires sorting of subjects.
			if (embed == Embed.LAST) {
				if (state.uniqueEmbeds.containsKey(id)) {
					removeEmbed(state, id);
				}
				state.uniqueEmbeds.put(id, new EmbedNode(parent, property));
			}

			state.subjectStack.push(id);

			// 5.5 If embed is @last or @always

			// Skip 5.5.1

			// 5.5.2 For each property and objects in node, ordered by property:
			final Map<String, Object> element = (Map<String, Object>) matches.get(id);
			List<String> props = new ArrayList<String>(element.keySet());
			Collections.sort(props);
			for (final String prop : props) {

				// 5.5.2.1 If property is a keyword, add property and objects to
				// output.
				if (isKeyword(prop)) {
					output.put(prop, JsonLdUtils.clone(element.get(prop)));
					continue;
				}

				// 5.5.2.2 Otherwise, if property is not in frame, and explicit
				// is true, processors
				// MUST NOT add any values for property to output, and the
				// following steps are skipped.
				if (explicitOn && !frame.containsKey(prop)) {
					continue;
				}

				// add objects
				final List<Object> value = (List<Object>) element.get(prop);

				// 5.5.2.3 For each item in objects:
				for (final Object item : value) {
					if ((item instanceof Map) && ((Map<String, Object>) item).containsKey(JsonLdConsts.LIST)) {
						// add empty list
						final Map<String, Object> list = newMap();
						list.put(JsonLdConsts.LIST, new ArrayList<Object>());
						addFrameOutput(state, output, prop, list);

						// add list objects
						for (final Object listitem : (List<Object>) ((Map<String, Object>) item)
								.get(JsonLdConsts.LIST)) {
							// 5.5.2.3.1.1 recurse into subject reference
							if (JsonLdUtils.isNodeReference(listitem)) {
								final Map<String, Object> tmp = newMap();
								final String itemid = (String) ((Map<String, Object>) listitem).get(JsonLdConsts.ID);
								// TODO: nodes may need to be node_map,
								// which is global
								tmp.put(itemid, this.nodeMap.get(itemid));
								Map<String, Object> subframe;
								if (frame.containsKey(prop)) {
									subframe = (Map<String, Object>) ((List<Object>) frame.get(prop)).get(0);
								} else {
									subframe = flags;
								}
								frame(state, tmp, subframe, list, JsonLdConsts.LIST);
							} else {

								// include other values automatcially (TODO:
								// may need JsonLdUtils.clone(n))
								addFrameOutput(state, list, JsonLdConsts.LIST, listitem);
							}
						}
					}
					// recurse into subject reference
					else if (JsonLdUtils.isNodeReference(item)) {
						final Map<String, Object> tmp = newMap();
						final String itemid = (String) ((Map<String, Object>) item).get(JsonLdConsts.ID);
						// TODO: nodes may need to be node_map, which is
						// global
						tmp.put(itemid, this.nodeMap.get(itemid));
						Map<String, Object> subframe;
						if (frame.containsKey(prop)) {
							subframe = (Map<String, Object>) ((List<Object>) frame.get(prop)).get(0);
						} else {
							subframe = flags;
						}
						frame(state, tmp, subframe, output, prop);
					} else {
						// include other values automatically (TODO: may
						// need JsonLdUtils.clone(o))
						addFrameOutput(state, output, prop, item);
					}
				}
			}

			// handle defaults
			props = new ArrayList<String>(frame.keySet());
			Collections.sort(props);
			for (final String prop : props) {
				// skip keywords
				if (isKeyword(prop)) {
					continue;
				}

				final List<Object> pf = (List<Object>) frame.get(prop);
				Map<String, Object> propertyFrame = pf.size() > 0 ? (Map<String, Object>) pf.get(0) : null;
				if (propertyFrame == null) {
					propertyFrame = newMap();
				}
				final boolean omitDefaultOn = getFrameFlag(propertyFrame, JsonLdConsts.OMIT_DEFAULT, state.omitDefault);
				if (!omitDefaultOn && !output.containsKey(prop)) {
					Object def = "@null";
					if (propertyFrame.containsKey(JsonLdConsts.DEFAULT)) {
						def = JsonLdUtils.clone(propertyFrame.get(JsonLdConsts.DEFAULT));
					}
					if (!(def instanceof List)) {
						final List<Object> tmp = new ArrayList<Object>();
						tmp.add(def);
						def = tmp;
					}
					final Map<String, Object> tmp1 = newMap(JsonLdConsts.PRESERVE, def);
					final List<Object> tmp2 = new ArrayList<Object>();
					tmp2.add(tmp1);
					output.put(prop, tmp2);
				}
			}

			// add output to parent
			addFrameOutput(state, parent, property, output);

			state.subjectStack.pop();
		}
	}

	private Object getFrameValue(Map<String, Object> frame, String name) {
		Object value = frame.get(name);
		if (value instanceof List) {
			if (((List<Object>) value).size() > 0) {
				value = ((List<Object>) value).get(0);
			}
		}
		if (value instanceof Map && ((Map<String, Object>) value).containsKey(JsonLdConsts.VALUE)) {
			value = ((Map<String, Object>) value).get(JsonLdConsts.VALUE);
		}
		return value;
	}

	private Boolean getFrameFlag(Map<String, Object> frame, String name, boolean thedefault) {
		final Object value = getFrameValue(frame, name);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return thedefault;
	}

	private Embed getFrameEmbed(Map<String, Object> frame, Embed thedefault) throws JsonLdError {
		final Object value = getFrameValue(frame, JsonLdConsts.EMBED);
		if (value == null) {
			return thedefault;
		}
		if (value instanceof Boolean) {
			return (Boolean) value ? Embed.LAST : Embed.NEVER;
		}
		if (value instanceof Embed) {
			return (Embed) value;
		}
		if (value instanceof String) {
			switch ((String) value) {
			case "@always":
				return Embed.ALWAYS;
			case "@never":
				return Embed.NEVER;
			case "@last":
				return Embed.LAST;
			case "@link":
				return Embed.LINK;
			default:
				throw new JsonLdError(JsonLdError.Error.INVALID_EMBED_VALUE);
			}
		}
		throw new JsonLdError(JsonLdError.Error.INVALID_EMBED_VALUE);
	}

	/**
	 * Removes an existing embed.
	 *
	 * @param state the current framing state.
	 * @param id    the @id of the embed to remove.
	 */
	private static void removeEmbed(FramingContext state, String id) {
		// get existing embed
		final Map<String, EmbedNode> links = state.uniqueEmbeds;
		final EmbedNode embed = links.get(id);
		final Object parent = embed.parent;
		final String property = embed.property;

		// create reference to replace embed
		final Map<String, Object> node = newMap(JsonLdConsts.ID, id);

		// remove existing embed
		if (JsonLdUtils.isNode(parent)) {
			// replace subject with reference
			final List<Object> newvals = new ArrayList<Object>();
			final List<Object> oldvals = (List<Object>) ((Map<String, Object>) parent).get(property);
			for (final Object v : oldvals) {
				if (v instanceof Map && Obj.equals(((Map<String, Object>) v).get(JsonLdConsts.ID), id)) {
					newvals.add(node);
				} else {
					newvals.add(v);
				}
			}
			((Map<String, Object>) parent).put(property, newvals);
		}
		// recursively remove dependent dangling embeds
		removeDependents(links, id);
	}

	private static void removeDependents(Map<String, EmbedNode> embeds, String id) {
		// get embed keys as a separate array to enable deleting keys in map
		for (final String id_dep : new HashSet<String>(embeds.keySet())) {
			final EmbedNode e = embeds.get(id_dep);
			if (e == null || e.parent == null || !(e.parent instanceof Map)) {
				continue;
			}
			final String pid = (String) ((Map<String, Object>) e.parent).get(JsonLdConsts.ID);
			if (Obj.equals(id, pid)) {
				embeds.remove(id_dep);
				removeDependents(embeds, id_dep);
			}
		}
	}

	private Map<String, Object> filterNodes(FramingContext state, Map<String, Object> nodes, Map<String, Object> frame,
			boolean requireAll) throws JsonLdError {
		final Map<String, Object> rval = newMap();
		for (final String id : nodes.keySet()) {
			final Map<String, Object> element = (Map<String, Object>) nodes.get(id);
			if (element != null && filterNode(state, element, frame, requireAll)) {
				rval.put(id, element);
			}
		}
		return rval;
	}

	private boolean filterNode(FramingContext state, Map<String, Object> node, Map<String, Object> frame,
			boolean requireAll) throws JsonLdError {
		final Object types = frame.get(JsonLdConsts.TYPE);
		final Object frameIds = frame.get(JsonLdConsts.ID);
		// https://json-ld.org/spec/latest/json-ld-framing/#frame-matching
		//
		// 1. Node matches if it has an @id property including any IRI or
		// blank node in the @id property in frame.
		if (frameIds != null) {
			if (frameIds instanceof String) {
				final Object nodeId = node.get(JsonLdConsts.ID);
				if (nodeId == null) {
					return false;
				}
				if (JsonLdUtils.deepCompare(nodeId, frameIds)) {
					return true;
				}
			} else if (frameIds instanceof LinkedHashMap && ((LinkedHashMap) frameIds).size() == 0) {
				if (node.containsKey(JsonLdConsts.ID)) {
					return true;
				}
				return false;
			} else if (!(frameIds instanceof List)) {
				throw new JsonLdError(Error.SYNTAX_ERROR, "frame @id must be an array");
			} else {
				final Object nodeId = node.get(JsonLdConsts.ID);
				if (nodeId == null) {
					return false;
				}
				for (final Object j : (List<Object>) frameIds) {
					if (JsonLdUtils.deepCompare(nodeId, j)) {
						return true;
					}
				}
			}
			return false;
		}
		// 2. Node matches if frame has no non-keyword properties.TODO
		// 3. If requireAll is true, node matches if all non-keyword properties
		// (property) in frame match any of the following conditions. Or, if
		// requireAll is false, if any of the non-keyword properties (property)
		// in frame match any of the following conditions. For the values of
		// each
		// property from frame in node:
		// 3.1 If property is @type:
		if (types != null) {
			if (!(types instanceof List)) {
				throw new JsonLdError(Error.SYNTAX_ERROR, "frame @type must be an array");
			}
			Object nodeTypes = node.get(JsonLdConsts.TYPE);
			if (nodeTypes == null) {
				nodeTypes = new ArrayList<Object>();
			} else if (!(nodeTypes instanceof List)) {
				throw new JsonLdError(Error.SYNTAX_ERROR, "node @type must be an array");
			}
			// 3.1.1 Property matches if the @type property in frame includes
			// any IRI in values.
			for (final Object i : (List<Object>) nodeTypes) {
				for (final Object j : (List<Object>) types) {
					if (JsonLdUtils.deepCompare(i, j)) {
						return true;
					}
				}
			}
			// TODO: 3.1.2
			// 3.1.3 Otherwise, property matches if values is empty and the
			// @type property in frame is match none.
			if (((List<Object>) types).size() == 1 && ((List<Object>) types).get(0) instanceof Map
					&& ((Map<String, Object>) ((List<Object>) types).get(0)).size() == 0) {
				return !((List<Object>) nodeTypes).isEmpty();
			}
			// 3.1.4 Otherwise, property does not match.
			return false;
		}
		// 3.2
		for (final String key : frame.keySet()) {
			if (!isKeyword(key) && !(node.containsKey(key))) {

				final Object frameObject = frame.get(key);
				if (frameObject instanceof ArrayList) {
					final ArrayList<Object> o = (ArrayList<Object>) frame.get(key);

					boolean _default = false;
					for (final Object oo : o) {
						if (oo instanceof Map) {
							if (((Map) oo).containsKey(JsonLdConsts.DEFAULT)) {
								_default = true;
							}
						}
					}
					if (_default) {
						continue;
					}
				}

				return false;
			}
		}
		return true;
	}

	/**
	 * Adds framing output to the given parent.
	 *
	 * @param state    the current framing state.
	 * @param parent   the parent to add to.
	 * @param property the parent property.
	 * @param output   the output to add.
	 */
	private static void addFrameOutput(FramingContext state, Object parent, String property, Object output) {
		if (parent instanceof Map) {
			List<Object> prop = (List<Object>) ((Map<String, Object>) parent).get(property);
			if (prop == null) {
				prop = new ArrayList<Object>();
				((Map<String, Object>) parent).put(property, prop);
			}
			prop.add(output);
		} else {
			((List) parent).add(output);
		}
	}

	/***
	 * ____ _ __ ____ ____ _____ _ _ _ _ _ / ___|___ _ ____ _____ _ __| |_ / _|_ __
	 * ___ _ __ ___ | _ \| _ \| ___| / \ | | __ _ ___ _ __(_) |_| |__ _ __ ___ | | /
	 * _ \| '_ \ \ / / _ \ '__| __| | |_| '__/ _ \| '_ ` _ \ | |_) | | | | |_ / _ \
	 * | |/ _` |/ _ \| '__| | __| '_ \| '_ ` _ \ | |__| (_) | | | \ V / __/ | | |_ |
	 * _| | | (_) | | | | | | | _ <| |_| | _| / ___ \| | (_| | (_) | | | | |_| | | |
	 * | | | | | \____\___/|_| |_|\_/ \___|_| \__| |_| |_| \___/|_| |_| |_| |_|
	 * \_\____/|_| /_/ \_\_|\__, |\___/|_| |_|\__|_| |_|_| |_| |_| |___/
	 */

	/**
	 * Helper class for node usages
	 *
	 * @author tristan
	 */
	private class UsagesNode {
		public UsagesNode(NodeMapNode node, String property, Map<String, Object> value) {
			this.node = node;
			this.property = property;
			this.value = value;
		}

		public NodeMapNode node = null;
		public String property = null;
		public Map<String, Object> value = null;
	}

	private class Node {
		private final String predicate;
		private final RDFDataset.Node object;

		public Node(String predicate, RDFDataset.Node object) {
			this.predicate = predicate;
			this.object = object;
		}
	}

	private class NodeMapNode extends LinkedHashMap<String, Object> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1601961618277557081L;
		public List<UsagesNode> usages = new ArrayList<>(4);

		public NodeMapNode(String id) {
			super();
			this.put(JsonLdConsts.ID, id);
		}

		// helper fucntion for 4.3.3
		public boolean isWellFormedListNode() {
			if (usages.size() != 1) {
				return false;
			}
			int keys = 0;
			if (containsKey(RDF_FIRST)) {
				keys++;
				if (!(get(RDF_FIRST) instanceof List && ((List<Object>) get(RDF_FIRST)).size() == 1)) {
					return false;
				}
			}
			if (containsKey(RDF_REST)) {
				keys++;
				if (!(get(RDF_REST) instanceof List && ((List<Object>) get(RDF_REST)).size() == 1)) {
					return false;
				}
			}
			if (containsKey(JsonLdConsts.TYPE)) {
				keys++;
				if (!(get(JsonLdConsts.TYPE) instanceof List && ((List<Object>) get(JsonLdConsts.TYPE)).size() == 1)
						&& RDF_LIST.equals(((List<Object>) get(JsonLdConsts.TYPE)).get(0))) {
					return false;
				}
			}
			// TODO: SPEC: 4.3.3 has no mention of @id
			if (containsKey(JsonLdConsts.ID)) {
				keys++;
			}
			if (keys < size()) {
				return false;
			}
			return true;
		}

		// return this node without the usages variable
		public Map<String, Object> serialize() {
			return new LinkedHashMap<String, Object>(this);
		}
	}

	/**
	 * Converts RDF statements into JSON-LD.
	 *
	 * @param dataset the RDF statements.
	 * @return A list of JSON-LD objects found in the given dataset.
	 * @throws JsonLdError If there was an error during conversion from RDF to
	 *                     JSON-LD.
	 */
	public List<Object> fromRDF(final RDFDataset dataset) throws JsonLdError {
		return fromRDF(dataset, false);
	}

	/**
	 * Converts RDF statements into JSON-LD, presuming that there are no duplicates
	 * in the dataset.
	 *
	 * @param dataset               the RDF statements.
	 * @param noDuplicatesInDataset True if there are no duplicates in the dataset
	 *                              and false otherwise.
	 * @return A list of JSON-LD objects found in the given dataset.
	 * @throws JsonLdError If there was an error during conversion from RDF to
	 *                     JSON-LD.
	 */
	public List<Object> fromRDF(final RDFDataset dataset, boolean noDuplicatesInDataset) throws JsonLdError {
		// 1)
		final Map<String, NodeMapNode> defaultGraph = new LinkedHashMap<String, NodeMapNode>(4);
		// 2)
		final Map<String, Map<String, NodeMapNode>> graphMap = new LinkedHashMap<String, Map<String, NodeMapNode>>(4);
		graphMap.put(JsonLdConsts.DEFAULT, defaultGraph);

		// 3/3.1)
		for (final String name : dataset.graphNames()) {

			final List<RDFDataset.Quad> graph = dataset.getQuads(name);

			// 3.2+3.4)
			final Map<String, NodeMapNode> nodeMap = graphMap.computeIfAbsent(name,
					k -> new LinkedHashMap<String, NodeMapNode>());

			// 3.3)
			if (!JsonLdConsts.DEFAULT.equals(name)) {
				// Existing entries in the default graph are not overwritten
				defaultGraph.computeIfAbsent(name, k -> new NodeMapNode(k));
			}

			// 3.5)
			final Map<String, List<Node>> nodes = new HashMap<>();

			for (final RDFDataset.Quad triple : graph) {
				final String subject = triple.getSubject().getValue();
				final String predicate = triple.getPredicate().getValue();
				final RDFDataset.Node object = triple.getObject();
				nodes.computeIfAbsent(subject, k -> new ArrayList<>()).add(new Node(predicate, object));
			}
			for (final Map.Entry<String, List<Node>> nodeEntry : nodes.entrySet()) {
				final String subject = nodeEntry.getKey();

				for (final Node n : nodeEntry.getValue()) {
					final String predicate = n.predicate;
					final RDFDataset.Node object = n.object;

					// 3.5.1+3.5.2)
					final NodeMapNode node = nodeMap.computeIfAbsent(subject, k -> new NodeMapNode(k));

					// 3.5.3)
					if ((object.isIRI() || object.isBlankNode())) {
						nodeMap.computeIfAbsent(object.getValue(), k -> new NodeMapNode(k));
					}

					// 3.5.4)
					if (RDF_TYPE.equals(predicate) && (object.isIRI() || object.isBlankNode()) && !opts.getUseRdfType()
							&& !nodes.containsKey(object.getValue())) {
						JsonLdUtils.mergeValue(node, JsonLdConsts.TYPE, object.getValue());
						continue;
					}

					// 3.5.5)
					final Map<String, Object> value = object.toObject(opts.getUseNativeTypes());

					// 3.5.6+7)
					if (noDuplicatesInDataset) {
						JsonLdUtils.laxMergeValue(node, predicate, value);
					} else {
						JsonLdUtils.mergeValue(node, predicate, value);
					}

					// 3.5.8)
					if (object.isBlankNode() || object.isIRI()) {
						// 3.5.8.1-3)
						nodeMap.get(object.getValue()).usages.add(new UsagesNode(node, predicate, value));
					}
				}
			}
		}

		// 4)
		for (final String name : graphMap.keySet()) {
			final Map<String, NodeMapNode> graph = graphMap.get(name);

			// 4.1)
			if (!graph.containsKey(RDF_NIL)) {
				continue;
			}

			// 4.2)
			final NodeMapNode nil = graph.get(RDF_NIL);
			// 4.3)
			for (final UsagesNode usage : nil.usages) {
				// 4.3.1)
				NodeMapNode node = usage.node;
				String property = usage.property;
				Map<String, Object> head = usage.value;
				// 4.3.2)
				final List<Object> list = new ArrayList<Object>(4);
				final List<String> listNodes = new ArrayList<String>(4);
				// 4.3.3)
				while (RDF_REST.equals(property) && node.isWellFormedListNode()) {
					// 4.3.3.1)
					list.add(((List<Object>) node.get(RDF_FIRST)).get(0));
					// 4.3.3.2)
					listNodes.add((String) node.get(JsonLdConsts.ID));
					// 4.3.3.3)
					final UsagesNode nodeUsage = node.usages.get(0);
					// 4.3.3.4)
					node = nodeUsage.node;
					property = nodeUsage.property;
					head = nodeUsage.value;
					// 4.3.3.5)
					if (!JsonLdUtils.isBlankNode(node)) {
						break;
					}
				}
				// 4.3.4)
				if (RDF_FIRST.equals(property)) {
					// 4.3.4.1)
					if (RDF_NIL.equals(node.get(JsonLdConsts.ID))) {
						continue;
					}
					// 4.3.4.3)
					final String headId = (String) head.get(JsonLdConsts.ID);
					// 4.3.4.4-5)
					head = (Map<String, Object>) ((List<Object>) graph.get(headId).get(RDF_REST)).get(0);
					// 4.3.4.6)
					list.remove(list.size() - 1);
					listNodes.remove(listNodes.size() - 1);
				}
				// 4.3.5)
				head.remove(JsonLdConsts.ID);
				// 4.3.6)
				Collections.reverse(list);
				// 4.3.7)
				head.put(JsonLdConsts.LIST, list);
				// 4.3.8)
				for (final String nodeId : listNodes) {
					graph.remove(nodeId);
				}
			}
		}

		// 5)
		final List<Object> result = new ArrayList<Object>(4);
		// 6)
		final List<String> ids = new ArrayList<String>(defaultGraph.keySet());
		Collections.sort(ids);
		for (final String subject : ids) {
			final NodeMapNode node = defaultGraph.get(subject);
			// 6.1)
			if (graphMap.containsKey(subject)) {
				// 6.1.1)
				final List<Object> nextGraph = new ArrayList<Object>(4);
				node.put(JsonLdConsts.GRAPH, nextGraph);
				// 6.1.2)
				final Map<String, NodeMapNode> nextSubjectMap = graphMap.get(subject);
				final List<String> keys = new ArrayList<String>(nextSubjectMap.keySet());
				Collections.sort(keys);
				for (final String s : keys) {
					final NodeMapNode n = nextSubjectMap.get(s);
					if (n.size() == 1 && n.containsKey(JsonLdConsts.ID)) {
						continue;
					}
					nextGraph.add(n.serialize());
				}
			}
			// 6.2)
			if (node.size() == 1 && node.containsKey(JsonLdConsts.ID)) {
				continue;
			}
			result.add(node.serialize());
		}

		return result;
	}

	/***
	 * ____ _ _ ____ ____ _____ _ _ _ _ _ / ___|___ _ ____ _____ _ __| |_ | |_ ___ |
	 * _ \| _ \| ___| / \ | | __ _ ___ _ __(_) |_| |__ _ __ ___ | | / _ \| '_ \ \ /
	 * / _ \ '__| __| | __/ _ \ | |_) | | | | |_ / _ \ | |/ _` |/ _ \| '__| | __| '_
	 * \| '_ ` _ \ | |__| (_) | | | \ V / __/ | | |_ | || (_) | | _ <| |_| | _| /
	 * ___ \| | (_| | (_) | | | | |_| | | | | | | | | \____\___/|_| |_|\_/ \___|_|
	 * \__| \__\___/ |_| \_\____/|_| /_/ \_\_|\__, |\___/|_| |_|\__|_| |_|_| |_| |_|
	 * |___/
	 */

	/**
	 * Adds RDF triples for each graph in the current node map to an RDF dataset.
	 *
	 * @return the RDF dataset.
	 * @throws JsonLdError If there was an error converting from JSON-LD to RDF.
	 */
	public RDFDataset toRDF() throws JsonLdError {
		// TODO: make the default generateNodeMap call (i.e. without a
		// graphName) create and return the nodeMap
		final Map<String, Object> nodeMap = newMap();
		nodeMap.put(JsonLdConsts.DEFAULT, newMap());
		generateNodeMap(this.value, nodeMap);

		final RDFDataset dataset = new RDFDataset(this);

		for (final String graphName : nodeMap.keySet()) {
			// 4.1)
			if (JsonLdUtils.isRelativeIri(graphName)) {
				continue;
			}
			final Map<String, Object> graph = (Map<String, Object>) nodeMap.get(graphName);
			dataset.graphToRDF(graphName, graph);
		}

		return dataset;
	}

	/***
	 * _ _ _ _ _ _ _ _ _ _ _ | \ | | ___ _ __ _ __ ___ __ _| (_)______ _| |_(_) ___
	 * _ __ / \ | | __ _ ___ _ __(_) |_| |__ _ __ ___ | \| |/ _ \| '__| '_ ` _ \ /
	 * _` | | |_ / _` | __| |/ _ \| '_ \ / _ \ | |/ _` |/ _ \| '__| | __| '_ \| '_ `
	 * _ \ | |\ | (_) | | | | | | | | (_| | | |/ / (_| | |_| | (_) | | | | / ___ \|
	 * | (_| | (_) | | | | |_| | | | | | | | | |_| \_|\___/|_| |_| |_|
	 * |_|\__,_|_|_/___\__,_|\__|_|\___/|_| |_| /_/ \_\_|\__, |\___/|_| |_|\__|_|
	 * |_|_| |_| |_| |___/
	 */

	/**
	 * Performs RDF normalization on the given JSON-LD input.
	 *
	 * @param dataset the expanded JSON-LD object to normalize.
	 * @return The normalized JSON-LD object
	 * @throws JsonLdError If there was an error while normalizing.
	 */
	public Object normalize(Map<String, Object> dataset) throws JsonLdError {
		// create quads and map bnodes to their associated quads
		final List<Object> quads = new ArrayList<Object>();
		final Map<String, Object> bnodes = newMap();
		for (String graphName : dataset.keySet()) {
			final List<Map<String, Object>> triples = (List<Map<String, Object>>) dataset.get(graphName);
			if (JsonLdConsts.DEFAULT.equals(graphName)) {
				graphName = null;
			}
			for (final Map<String, Object> quad : triples) {
				if (graphName != null) {
					if (graphName.indexOf("_:") == 0) {
						final Map<String, Object> tmp = newMap();
						tmp.put("type", "blank node");
						tmp.put("value", graphName);
						quad.put("name", tmp);
					} else {
						final Map<String, Object> tmp = newMap();
						tmp.put("type", "IRI");
						tmp.put("value", graphName);
						quad.put("name", tmp);
					}
				}
				quads.add(quad);

				final String[] attrs = new String[] { "subject", "object", "name" };
				for (final String attr : attrs) {
					if (quad.containsKey(attr)
							&& "blank node".equals(((Map<String, Object>) quad.get(attr)).get("type"))) {
						final String id = (String) ((Map<String, Object>) quad.get(attr)).get("value");
						if (!bnodes.containsKey(id)) {
							bnodes.put(id, new LinkedHashMap<String, List<Object>>() {
								/**
								 * 
								 */
								private static final long serialVersionUID = -3809863660799245111L;

								{
									put("quads", new ArrayList<Object>());
								}
							});
						}
						((List<Object>) ((Map<String, Object>) bnodes.get(id)).get("quads")).add(quad);
					}
				}
			}
		}

		// mapping complete, start canonical naming
		final NormalizeUtils normalizeUtils = new NormalizeUtils(quads, bnodes, new UniqueNamer("_:c14n"), opts);
		return normalizeUtils.hashBlankNodes(bnodes.keySet());
	}

	/*
	 * public Map<String, Object> expandWithCoreContext(Object geoJsonValue) {
	 * return expand(JsonLdProcessor.getCoreContextClone(), geoJsonValue, -1,
	 * false); }
	 */

}