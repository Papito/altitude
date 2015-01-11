/* 
 * ===========================
 * Extra ko binding handlers
 * =========================== 
 */

// Table sort
// usage: <th data-bind="sort: { arr: posts, prop: 'nbhd()' }">Neighborhood</th>
ko.bindingHandlers.sort = {
    init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
        var asc = false;
        element.style.cursor = 'pointer';

        element.onclick = function(){
            var value = valueAccessor();
            var prop = value.prop;
            var data = value.arr;

            asc = !asc;

            // add up/down sort indicator
            // assumes bootstrap and jquery available
            $("th span").remove(".glyphicon-chevron-up").remove(".glyphicon-chevron-down");
            if (asc) {
                $(element).append(' <span class="glyphicon glyphicon-chevron-up"></span>');
            }
            else {
                $(element).append(' <span class="glyphicon glyphicon-chevron-down"></span>');
            }

            data.sort(function(left, right) {
                var rec1 = left;
                var rec2 = right;

                if(!asc) {
                    rec1 = right;
                    rec2 = left;
                }

                var props = prop.split('.');
                for(var i in props){
                    var propName = props[i];
                    var parenIndex = propName.indexOf('()');
                    if(parenIndex > 0){
                        propName = propName.substring(0, parenIndex);
                        rec1 = rec1[propName]();
                        rec2 = rec2[propName]();
                    } else {
                        rec1 = rec1[propName];
                        rec2 = rec2[propName];
                    }
                }

                return rec1 == rec2 ? 0 : rec1 < rec2 ? -1 : 1;
            });
        };
    }
};