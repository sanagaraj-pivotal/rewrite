/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.config.RecipeDescriptor;

import java.util.*;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class RecipesThatMadeChanges implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    Collection<Stack<Recipe>> recipes;

    public static RecipesThatMadeChanges create(Stack<Recipe> recipeStack) {
        List<Stack<Recipe>> recipeStackList = new ArrayList<>(1);
        recipeStackList.add(recipeStack);
        return new RecipesThatMadeChanges(Tree.randomId(), recipeStackList);
    }
}
