var backgroundColors = [
    'rgba(255, 99, 132, 0.2)',
    'rgba(54, 162, 235, 0.2)',
    'rgba(255, 206, 86, 0.2)',
    'rgba(75, 192, 192, 0.2)',
    'rgba(153, 102, 255, 0.2)',
    'rgba(255, 159, 64, 0.2)'
];
var borderColors = [
    'rgba(255, 99, 132, 1)',
    'rgba(54, 162, 235, 1)',
    'rgba(255, 206, 86, 1)',
    'rgba(75, 192, 192, 1)',
    'rgba(153, 102, 255, 1)',
    'rgba(255, 159, 64, 1)'
]
var idx = 0;
for (let germ in window.data) {
    idx++;
    let labels2 = [];
    let data2 = [];
    for (let year in data[germ]) {
        labels2.push(year)
        data2.push(data[germ][year])
    }
    const ctx = document.getElementById('myChart' + germ).getContext('2d');
    const myChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels2,
            datasets: [{
                label: "Anzahl " + germ,
                data: data2,
                backgroundColor: backgroundColors[idx],
                borderColor: borderColors[idx],
                borderWidth: 1
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

