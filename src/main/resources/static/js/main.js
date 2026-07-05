/**
 * Leave & Payroll Portal Front-end Actions
 */
document.addEventListener('DOMContentLoaded', function() {
    
    // Live Leave Duration & LOP Calculator
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const liveDaysCalculator = document.getElementById('liveDaysCalculator');
    const calculatedDays = document.getElementById('calculatedDays');
    const exceedWarning = document.getElementById('exceedWarning');
    const lopDaysCalc = document.getElementById('lopDaysCalc');

    function calculateDays() {
        const startVal = startDateInput.value;
        const endVal = endDateInput.value;

        if (startVal && endVal) {
            const start = new Date(startVal);
            const end = new Date(endVal);

            // Reset time part to prevent daylight saving issues
            start.setHours(0,0,0,0);
            end.setHours(0,0,0,0);

            if (end >= start) {
                // Calculate difference in days
                const diffTime = end.getTime() - start.getTime();
                const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
                
                calculatedDays.textContent = diffDays;
                liveDaysCalculator.classList.remove('d-none');

                // Check if remainingLeaves is set in scope
                if (typeof remainingLeaves !== 'undefined') {
                    if (diffDays > remainingLeaves) {
                        const lop = diffDays - remainingLeaves;
                        lopDaysCalc.textContent = lop;
                        exceedWarning.classList.remove('d-none');
                    } else {
                        exceedWarning.classList.add('d-none');
                    }
                }
            } else {
                liveDaysCalculator.classList.add('d-none');
            }
        } else {
            liveDaysCalculator.classList.add('d-none');
        }
    }

    if (startDateInput && endDateInput) {
        startDateInput.addEventListener('change', calculateDays);
        endDateInput.addEventListener('change', calculateDays);
    }

    // Real-time Employee Directory Search Filter
    const employeeSearchInput = document.getElementById('employeeSearchInput');
    const employeeTable = document.getElementById('employeeTable');

    if (employeeSearchInput && employeeTable) {
        employeeSearchInput.addEventListener('input', function() {
            const query = this.value.toLowerCase().trim();
            const rows = employeeTable.querySelectorAll('tbody tr');
            
            rows.forEach(row => {
                const text = row.textContent.toLowerCase();
                if (text.includes(query)) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        });
    }
});
