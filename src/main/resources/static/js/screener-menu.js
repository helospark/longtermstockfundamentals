

function handleMetricChange(selectElement) {
    const row = selectElement.closest('.condition-row');
    const container = row.querySelector('.value-control-container');
    const inputWrapper = container.querySelector('.input-wrapper');
    const tagsContainer = container.querySelector('.tags-container');
    const operatorDropdown = row.querySelector('.operator-dropdown');
    
    // Wipe prior tag state allocations
    tagsContainer.innerHTML = '';

    const selectedOption = selectElement.options[selectElement.selectedIndex];
    const allowedValuesRaw = selectedOption.getAttribute('data-allowed-values');

    if (allowedValuesRaw && allowedValuesRaw.trim() !== '') {
        try {
            if (operatorDropdown) {
                operatorDropdown.value = "in";
            }
        
            const allowedValues = JSON.parse(allowedValuesRaw);
            
            // Build the multi-select dynamic dropdown markup
            let selectHtml = `<select class="allowed-values-dropdown" onchange="handleTagSelection(this)">`;
            selectHtml += `<option value="" disabled selected>Select...</option>`;
            
            for (const [key, value] of Object.entries(allowedValues)) {
                selectHtml += `<option value="${value}">${key}</option>`;
            }
            selectHtml += `</select>`;
            
            inputWrapper.innerHTML = selectHtml;
        } catch (e) {
            fallbackToTextInput(inputWrapper);
        }
    } else {
        if (operatorDropdown) {
            operatorDropdown.value = ">";
        }
        fallbackToTextInput(inputWrapper);
    }
}

function fallbackToTextInput(wrapper) {
    wrapper.innerHTML = `<input class="numeric-value-input" value="30.0"></input>`;
}

function handleTagSelection(dropdown) {
    if (dropdown.selectedIndex === 0) return;

    const selectedOption = dropdown.options[dropdown.selectedIndex];
    const label = selectedOption.text;
    const id = dropdown.value; // This is your Integer ID string (e.g., "5")
    const tagsContainer = dropdown.closest('.value-control-container').querySelector('.tags-container');

    // Duplicate check
    const existingTags = tagsContainer.querySelectorAll('.badge-tag');
    for (let tag of existingTags) {
        if (tag.getAttribute('data-id') === id) {
            dropdown.selectedIndex = 0;
            return;
        }
    }

    const tagSpan = document.createElement('span');
    tagSpan.className = 'badge bg-primary me-1 mb-1 d-inline-flex align-items-center badge-tag';
    tagSpan.style.cssText = 'padding: 5px 8px; margin-right: 4px; display: inline-block; font-size: 12px; background: #007bff; color: white; border-radius: 4px;';
    
    // Store it directly on the node
    tagSpan.setAttribute('data-id', id);
    tagSpan.setAttribute('data-label', label);
    tagSpan.innerHTML = `${label} <a href="javascript:;" onclick="this.parentElement.remove()" style="color: white; margin-left: 6px; text-decoration: none; font-weight: bold;">&times;</a>`;

    tagsContainer.appendChild(tagSpan);
    dropdown.selectedIndex = 0;
}
