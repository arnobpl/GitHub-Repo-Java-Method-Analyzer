package jsonUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * This class uses a mechanism for filtering JsonArray.
 */
public abstract class JsonArrayFilterer {
    //region variables

    private final JsonArray givenJsonArray;

    //endregion


    //region constructors

    public JsonArrayFilterer(JsonArray givenJsonArray) {
        this.givenJsonArray = givenJsonArray;
    }

    //endregion


    //region abstract methods

    /**
     * This method should be implemented to create a filter.
     *
     * @param jsonElement iterated jsonElement
     * @return true if jsonElement should be included in filtered jsonArray
     */
    public abstract boolean shouldBeIncluded(JsonElement jsonElement);

    //endregion


    //region methods

    /**
     * This method uses {@code shouldBeIncluded} for filtering.
     *
     * @return filtered jsonArray
     */
    public JsonArray filterJsonArray() {
        JsonArray filteredJsonArray = new JsonArray(givenJsonArray.size());

        for (JsonElement jsonElement : givenJsonArray) {
            if (shouldBeIncluded(jsonElement))
                filteredJsonArray.add(jsonElement);
        }

        return filteredJsonArray;
    }

    //endregion
}
